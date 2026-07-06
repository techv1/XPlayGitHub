// PLACEHOLDER_FOR_BASE64_MAP

const STORED_VIDEOS = [
  {
    id: "DoAJJl4D6GikXv6",
    title: "Wicked Super Naturals",
    description: "Streamable media file loaded from the stored XPlay catalog.",
    thumbnail_filename: "Thumb_Wicked - Super Naturals.mp4.jpg",
    duration_sec: 600,
    quality: "HD",
    genres: ["Sci-Fi", "Action", "Cyberpunk"],
    year: 2026,
    rating: 8.8
  },
  {
    id: "kkvaaBmObjCOVZ4",
    title: "TeensLoveBlackCocks Hard Black Cock After a Hard Sell",
    description: "Streamable media file loaded from the stored XPlay catalog.",
    thumbnail_filename: "Thumb_TeensLoveBlackCocks - Hard Black Cock After a Hard Sell.mp4.jpg",
    duration_sec: 600,
    quality: "HD",
    genres: ["Sci-Fi", "Thriller", "Adventure"],
    year: 2026,
    rating: 8.5
  },
  {
    id: "gdvbbPzMG3fqKaR",
    title: "Elektra",
    description: "Streamable media file loaded from the stored XPlay catalog.",
    thumbnail_filename: "Thumb_Elektra.mp4.jpg",
    duration_sec: 600,
    quality: "HD",
    genres: ["Action", "Thriller", "Suspense"],
    year: 2026,
    rating: 8.2
  },
  {
    id: "kK8r7ZmVeDuOK7O",
    title: "BBCSurprise Devon",
    description: "Streamable media file loaded from the stored XPlay catalog.",
    thumbnail_filename: "Thumb_BBCSurprise - Devon.mp4.jpg",
    duration_sec: 600,
    quality: "HD",
    genres: ["Fantasy", "Drama", "Mystery"],
    year: 2026,
    rating: 7.9
  },
  {
    id: "Q304xa28zlF0Mr3",
    title: "Sex With My Younger Sister 4",
    description: "Streamable media file loaded from the stored XPlay catalog.",
    thumbnail_filename: "Thumb_Sex With My Younger Sister 4.mp4.jpg",
    duration_sec: 600,
    quality: "HD",
    genres: ["Drama", "Sci-Fi", "Melodrama"],
    year: 2026,
    rating: 7.6
  }
];

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);
    const path = url.pathname;
    const cache = caches.default;
    
    const login = env.STREAMTAPE_LOGIN || "67039df88009a5123291";
    const key = env.STREAMTAPE_KEY || "PJk0Og38oJF027z";

    // CORS Headers for API requests
    const corsHeaders = {
      "Access-Control-Allow-Origin": "*",
      "Access-Control-Allow-Methods": "GET, HEAD, POST, OPTIONS",
      "Access-Control-Allow-Headers": "Content-Type",
    };

    if (request.method === "OPTIONS") {
      return new Response(null, { headers: corsHeaders });
    }

    // ── 1. Embedded Thumbnail Server (Returns base64 images directly) ──
    if (path.startsWith("/v1/thumbnail/")) {
      const filename = decodeURIComponent(path.split("/v1/thumbnail/")[1]);
      const base64Data = EMBEDDED_THUMBNAILS[filename];
      
      if (!base64Data) {
        return new Response("Thumbnail not found", { status: 404, headers: corsHeaders });
      }

      // Convert Base64 string to binary bytes
      const binaryString = atob(base64Data);
      const len = binaryString.length;
      const bytes = new Uint8Array(len);
      for (let i = 0; i < len; i++) {
        bytes[i] = binaryString.charCodeAt(i);
      }

      return new Response(bytes, {
        headers: {
          "Content-Type": "image/jpeg",
          "Cache-Control": "public, max-age=31536000", // Cache forever (1 year)
          ...corsHeaders
        }
      });
    }

    // ── 2. Resolve link (Self-Healing Auto-Retry for Streamtape rate limiting) ──
    if (path.startsWith("/v1/resolve/")) {
      const fileId = path.split("/v1/resolve/")[1];
      
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
              await new Promise(resolve => setTimeout(resolve, waitTime * 1000));
            }
            break; // Success
          }
          
          const errorMsg = ticketData.msg || "";
          if (errorMsg.includes("wait") || ticketData.status === 403 || ticketData.status === 429) {
            // Extract seconds to wait from message (e.g. "You need to wait 3 more seconds")
            const match = errorMsg.match(/wait\s+(\d+)\s+more/i);
            const waitSeconds = match ? parseInt(match[1]) : 5;
            console.log(`Rate limited on ticket request. Pausing for ${waitSeconds + 1}s before retry...`);
            await new Promise(resolve => setTimeout(resolve, (waitSeconds + 1) * 1000));
            ticketAttempt++;
          } else {
            return new Response(JSON.stringify({ error: errorMsg }), { status: 500, headers: corsHeaders });
          }
        }

        if (!ticket) {
          return new Response(JSON.stringify({ error: "Streamtape API rate limit exceeded after retries" }), { status: 429, headers: corsHeaders });
        }

        // Fetch direct link using resolved ticket
        const dlRes = await fetch(`https://api.streamtape.com/file/dl?file=${fileId}&ticket=${ticket}`);
        const dlData = await dlRes.json();
        if (dlData.status !== 200) {
          return new Response(JSON.stringify({ error: dlData.msg }), { status: 500, headers: corsHeaders });
        }

        const response = new Response(JSON.stringify({ stream_url: dlData.result.url }), {
          headers: {
            "Content-Type": "application/json",
            "Cache-Control": "public, max-age=480", // 8 minutes cache
            ...corsHeaders
          }
        });

        ctx.waitUntil(cache.put(request, response.clone()));
        return response;
      } catch (err) {
        return new Response(JSON.stringify({ error: err.message }), { status: 500, headers: corsHeaders });
      }
    }

    // Helper: read all videos from the stored worker catalog.
    async function getVideosList() {
      return STORED_VIDEOS.map((video) => {
        const imageUrl = EMBEDDED_THUMBNAILS[video.thumbnail_filename]
          ? `${url.origin}/v1/thumbnail/${encodeURIComponent(video.thumbnail_filename)}`
          : "https://images.unsplash.com/photo-1578894381163-e72c17f2d45f?q=80&w=600";

        return {
          id: video.id,
          title: video.title,
          description: video.description,
          thumbnail_url: imageUrl,
          backdrop_url: imageUrl,
          stream_url: `https://streamtape.com/v/${video.id}`,
          duration_sec: video.duration_sec,
          quality: video.quality,
          genres: video.genres,
          year: video.year,
          rating: video.rating
        };
      });
    }

    // ── 3. Home Feed ──
    if (path === "/v1/home") {
      try {
        const videosList = await getVideosList();
        const feed = [
          { id: "cat_cloud", name: "Streamtape Cloud Videos", videos: videosList },
          { id: "cat_trending", name: "Trending Now", videos: videosList.slice(0, 3) }
        ];
        return new Response(JSON.stringify(feed), {
          headers: { "Content-Type": "application/json", "Cache-Control": "public, max-age=300", ...corsHeaders }
        });
      } catch (err) {
        return new Response(JSON.stringify({ error: err.message }), { status: 500, headers: corsHeaders });
      }
    }

    // ── 4. Video Details ──
    if (path.startsWith("/v1/videos/")) {
      const videoId = path.split("/v1/videos/")[1];
      try {
        const videosList = await getVideosList();
        const video = videosList.find(v => v.id === videoId);
        if (!video) return new Response(JSON.stringify({ error: "Video not found" }), { status: 404, headers: corsHeaders });

        return new Response(JSON.stringify(video), {
          headers: { "Content-Type": "application/json", ...corsHeaders }
        });
      } catch (err) {
        return new Response(JSON.stringify({ error: err.message }), { status: 500, headers: corsHeaders });
      }
    }

    // ── 5. Search ──
    if (path === "/v1/search") {
      const query = url.searchParams.get("q") || "";
      try {
        const videosList = await getVideosList();
        const filtered = videosList.filter(v => 
          v.title.toLowerCase().includes(query.toLowerCase()) || 
          v.description.toLowerCase().includes(query.toLowerCase())
        );
        return new Response(JSON.stringify(filtered), {
          headers: { "Content-Type": "application/json", ...corsHeaders }
        });
      } catch (err) {
        return new Response(JSON.stringify({ error: err.message }), { status: 500, headers: corsHeaders });
      }
    }

    // ── 6. Recommendations ──
    if (path === "/v1/recommendations") {
      try {
        const videosList = await getVideosList();
        return new Response(JSON.stringify(videosList), {
          headers: { "Content-Type": "application/json", ...corsHeaders }
        });
      } catch (err) {
        return new Response(JSON.stringify({ error: err.message }), { status: 500, headers: corsHeaders });
      }
    }

    // ── 7. Trending ──
    if (path === "/v1/trending") {
      try {
        const videosList = await getVideosList();
        return new Response(JSON.stringify(videosList), {
          headers: { "Content-Type": "application/json", ...corsHeaders }
        });
      } catch (err) {
        return new Response(JSON.stringify({ error: err.message }), { status: 500, headers: corsHeaders });
      }
    }

    return new Response("Not Found", { status: 404, headers: corsHeaders });
  }
};

function cleanName(raw) {
  return raw.substring(0, raw.lastIndexOf('.'))
    .replace(/_/g, ' ')
    .replace(/-/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();
}
