const CATALOG_KEY = "catalog:v1";
const DEFAULT_DURATION = 600;
const DEFAULT_QUALITY = "HD";
const DEFAULT_GENRES = ["Drama", "Sci-Fi", "Thriller"];
const DEFAULT_YEAR = 2026;
const DEFAULT_RATING = 8.0;

function corsHeaders() {
  return {
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Methods": "GET, HEAD, POST, OPTIONS",
    "Access-Control-Allow-Headers": "Content-Type",
  };
}

function jsonResponse(body, status = 200, extraHeaders = {}) {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      "Content-Type": "application/json",
      ...corsHeaders(),
      ...extraHeaders,
    },
  });
}

function cleanTitle(filename) {
  return filename
    .replace(/\.mp4$/i, "")
    .replace(/\.mkv$/i, "")
    .replace(/_/g, " ")
    .replace(/\s+/g, " ")
    .trim();
}

async function fetchJson(url) {
  const res = await fetch(url);
  if (!res.ok) {
    throw new Error(`HTTP ${res.status} from ${url}`);
  }
  return res.json();
}

async function refreshCatalog(env) {
  const login = env.STREAMTAPE_LOGIN;
  const key = env.STREAMTAPE_KEY;

  const listUrl = `https://api.streamtape.com/file/listfolder?login=${login}&key=${key}`;
  const listData = await fetchJson(listUrl);

  if (listData.status !== 200 || !listData.result || !listData.result.files) {
    throw new Error(listData.msg || "Failed to list Streamtape folder");
  }

  const files = listData.result.files.filter((f) => f.convert === "converted");
  const thumbnails = {};
  const videos = [];

  for (const file of files) {
    const fileId = file.linkid;
    const title = cleanTitle(file.name);
    const thumbnailFilename = `Thumb_${file.name}.jpg`;

    let thumbnailUrl = null;
    try {
      const splashUrl = `https://api.streamtape.com/file/getsplash?login=${login}&key=${key}&file=${fileId}`;
      const splashData = await fetchJson(splashUrl);
      if (splashData.status === 200 && splashData.result) {
        thumbnailUrl = splashData.result;
      }
    } catch (e) {
      console.error(`Failed to fetch thumbnail for ${fileId}: ${e.message}`);
    }

    if (thumbnailUrl) {
      thumbnails[fileId] = thumbnailUrl;
    }

    videos.push({
      id: fileId,
      title: title,
      description: "Streamable media file loaded from the stored XPlay catalog.",
      thumbnail_url: thumbnailUrl || `https://streamtape.com/v/${fileId}`,
      backdrop_url: thumbnailUrl || `https://streamtape.com/v/${fileId}`,
      stream_url: `https://streamtape.com/v/${fileId}`,
      duration_sec: DEFAULT_DURATION,
      quality: DEFAULT_QUALITY,
      genres: DEFAULT_GENRES,
      year: DEFAULT_YEAR,
      rating: DEFAULT_RATING,
      thumbnail_filename: thumbnailFilename,
    });
  }

  videos.sort((a, b) => a.title.localeCompare(b.title));

  const catalog = {
    refreshedAt: new Date().toISOString(),
    videos: videos,
    thumbnails: thumbnails,
  };

  await env.XPLAY_CATALOG.put(CATALOG_KEY, JSON.stringify(catalog));
  return catalog;
}

async function getCatalog(env) {
  const stored = await env.XPLAY_CATALOG.get(CATALOG_KEY);
  if (!stored) {
    return null;
  }
  return JSON.parse(stored);
}

async function handleRequest(request, env, ctx) {
  const url = new URL(request.url);
  const path = url.pathname;
  const cache = caches.default;

  if (request.method === "OPTIONS") {
    return new Response(null, { headers: corsHeaders() });
  }

  // ── 1. Admin HTML page ──
  if (path === "/v1/admin" && request.method === "GET") {
    const catalog = await getCatalog(env);
    const lastRefresh = catalog?.refreshedAt
      ? new Date(catalog.refreshedAt).toLocaleString()
      : "Never";
    const videoCount = catalog?.videos?.length || 0;

    const html = `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>XPlay Admin</title>
  <style>
    body { font-family: system-ui, sans-serif; max-width: 600px; margin: 60px auto; padding: 20px; }
    h1 { color: #e50914; }
    button { background: #e50914; color: white; border: none; padding: 14px 28px; font-size: 16px; border-radius: 6px; cursor: pointer; }
    button:disabled { background: #999; cursor: not-allowed; }
    #status { margin-top: 20px; padding: 12px; border-radius: 6px; }
    .info { color: #555; margin: 10px 0; }
  </style>
</head>
<body>
  <h1>XPlay Admin</h1>
  <p class="info">Last refresh: <strong>${lastRefresh}</strong></p>
  <p class="info">Videos in catalog: <strong>${videoCount}</strong></p>
  <button id="refreshBtn" onclick="refreshCatalog()">Update Catalog Now</button>
  <div id="status"></div>
  <script>
    async function refreshCatalog() {
      const btn = document.getElementById('refreshBtn');
      const status = document.getElementById('status');
      btn.disabled = true;
      status.innerText = 'Refreshing...';
      try {
        const res = await fetch('/v1/admin/refresh', { method: 'POST' });
        const data = await res.json();
        if (res.ok) {
          status.innerHTML = '<p style="color: green;">✅ Refreshed ' + data.videos + ' videos at ' + new Date(data.refreshedAt).toLocaleString() + '</p>';
        } else {
          status.innerHTML = '<p style="color: red;">❌ Error: ' + (data.error || 'Unknown error') + '</p>';
        }
      } catch (e) {
        status.innerHTML = '<p style="color: red;">❌ Error: ' + e.message + '</p>';
      } finally {
        btn.disabled = false;
      }
    }
  </script>
</body>
</html>`;

    return new Response(html, {
      headers: { "Content-Type": "text/html", ...corsHeaders() },
    });
  }

  // ── 2. Manual refresh trigger ──
  if (path === "/v1/admin/refresh" && request.method === "POST") {
    try {
      const catalog = await refreshCatalog(env);
      return jsonResponse({
        success: true,
        videos: catalog.videos.length,
        refreshedAt: catalog.refreshedAt,
      });
    } catch (err) {
      console.error("Refresh failed:", err);
      return jsonResponse({ error: err.message }, 502);
    }
  }

  // ── 3. Thumbnail proxy/redirect ──
  if (path.startsWith("/v1/thumbnail/")) {
    const fileId = path.split("/v1/thumbnail/")[1];
    const catalog = await getCatalog(env);
    const remoteUrl = catalog?.thumbnails?.[fileId];

    if (!remoteUrl) {
      return new Response("Thumbnail not found", { status: 404, headers: corsHeaders() });
    }

    return new Response(null, {
      status: 302,
      headers: {
        Location: remoteUrl,
        "Cache-Control": "public, max-age=14400", // 4 hours
        ...corsHeaders(),
      },
    });
  }

  // ── 4. Resolve direct Streamtape link (kept from original) ──
  if (path.startsWith("/v1/resolve/")) {
    const fileId = path.split("/v1/resolve/")[1];
    const login = env.STREAMTAPE_LOGIN;
    const key = env.STREAMTAPE_KEY;

    let cachedResponse = await cache.match(request);
    if (cachedResponse) return cachedResponse;

    try {
      let ticket = null;
      let ticketAttempt = 0;
      const maxAttempts = 3;

      while (ticketAttempt < maxAttempts) {
        const ticketRes = await fetch(`https://api.streamtape.com/file/dlticket?file=${fileId}&login=${login}&key=${key}`);
        const ticketData = await ticketRes.json();

        if (ticketData.status === 200 && ticketData.result && ticketData.result.ticket) {
          ticket = ticketData.result.ticket;
          const waitTime = ticketData.result.wait_time || 0;
          if (waitTime > 0) {
            await new Promise((resolve) => setTimeout(resolve, waitTime * 1000));
          }
          break;
        }

        const errorMsg = ticketData.msg || "";
        if (errorMsg.includes("wait") || ticketData.status === 403 || ticketData.status === 429) {
          const match = errorMsg.match(/wait\s+(\d+)\s+more/i);
          const waitSeconds = match ? parseInt(match[1]) : 5;
          console.log(`Rate limited on ticket request. Pausing for ${waitSeconds + 1}s before retry...`);
          await new Promise((resolve) => setTimeout(resolve, (waitSeconds + 1) * 1000));
          ticketAttempt++;
        } else {
          return jsonResponse({ error: errorMsg }, 500);
        }
      }

      if (!ticket) {
        return jsonResponse({ error: "Streamtape API rate limit exceeded after retries" }, 429);
      }

      const dlRes = await fetch(`https://api.streamtape.com/file/dl?file=${fileId}&ticket=${ticket}`);
      const dlData = await dlRes.json();
      if (dlData.status !== 200) {
        return jsonResponse({ error: dlData.msg }, 500);
      }

      const response = new Response(JSON.stringify({ stream_url: dlData.result.url }), {
        headers: {
          "Content-Type": "application/json",
          "Cache-Control": "public, max-age=480", // 8 minutes
          ...corsHeaders(),
        },
      });

      ctx.waitUntil(cache.put(request, response.clone()));
      return response;
    } catch (err) {
      return jsonResponse({ error: err.message }, 500);
    }
  }

  // Helper to read catalog
  const catalog = await getCatalog(env);
  if (!catalog) {
    return jsonResponse({ error: "Catalog not initialized. Visit /v1/admin and click Update Catalog." }, 503);
  }
  const videosList = catalog.videos;

  // ── 5. Home Feed ──
  if (path === "/v1/home") {
    const feed = [
      { id: "cat_cloud", name: "Streamtape Cloud Videos", videos: videosList },
      { id: "cat_trending", name: "Trending Now", videos: videosList.slice(0, 3) },
    ];
    return jsonResponse(feed, 200, { "Cache-Control": "public, max-age=300" });
  }

  // ── 6. Video Details ──
  if (path.startsWith("/v1/videos/")) {
    const videoId = path.split("/v1/videos/")[1];
    const video = videosList.find((v) => v.id === videoId);
    if (!video) {
      return jsonResponse({ error: "Video not found" }, 404);
    }
    return jsonResponse(video);
  }

  // ── 7. Search ──
  if (path === "/v1/search") {
    const query = url.searchParams.get("q") || "";
    const filtered = videosList.filter(
      (v) =>
        v.title.toLowerCase().includes(query.toLowerCase()) ||
        v.description.toLowerCase().includes(query.toLowerCase())
    );
    return jsonResponse(filtered);
  }

  // ── 8. Recommendations ──
  if (path === "/v1/recommendations") {
    return jsonResponse(videosList);
  }

  // ── 9. Trending ──
  if (path === "/v1/trending") {
    return jsonResponse(videosList);
  }

  return new Response("Not Found", { status: 404, headers: corsHeaders() });
}

export default {
  async fetch(request, env, ctx) {
    return handleRequest(request, env, ctx);
  },

  async scheduled(event, env, ctx) {
    console.log("Cron triggered at", new Date().toISOString());
    ctx.waitUntil(
      refreshCatalog(env).catch((err) => {
        console.error("Scheduled refresh failed:", err);
      })
    );
  },
};
