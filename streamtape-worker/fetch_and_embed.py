import os
import base64
import time
from ftplib import FTP

def run():
    print("Connecting to FTP ftp.streamtape.com...")
    ftp = FTP('ftp.streamtape.com')
    ftp.login(user='67039df88009a5123291', passwd='PJk0Og38oJF027z')
    ftp.set_pasv(True) # Use Passive Mode for stable transfer
    
    print("Changing to Thumbnails directory...")
    ftp.cwd('Thumbnails')
    
    lines = []
    ftp.retrlines('LIST', lines.append)
    
    print(f"Listed {len(lines)} lines.")
    
    embedded_map = {}
    for line in lines:
        if not line.strip():
            continue
        
        parts = line.split(None, 8)
        if len(parts) < 9:
            continue
            
        filename = parts[8].strip()
        if not filename.endswith('.jpg'):
            continue
            
        print(f"Downloading {filename}...")
        
        # Download with up to 3 retries, re-establishing FTP if needed
        data = bytearray()
        for attempt in range(3):
            try:
                # Ensure connection is alive
                try:
                    ftp.voidcmd("NOOP")
                except:
                    print("Reconnecting to FTP...")
                    ftp = FTP('ftp.streamtape.com')
                    ftp.login(user='67039df88009a5123291', passwd='PJk0Og38oJF027z')
                    ftp.set_pasv(True)
                    ftp.cwd('Thumbnails')
                
                data = bytearray()
                ftp.retrbinary(f'RETR {filename}', data.extend)
                if len(data) > 0:
                    break
                print(f"Empty download for {filename}, retrying in 2s...")
                time.sleep(2)
            except Exception as e:
                print(f"Error on attempt {attempt + 1} for {filename}: {e}")
                time.sleep(2)
                
        if len(data) > 0:
            b64_str = base64.b64encode(data).decode('utf-8')
            embedded_map[filename] = b64_str
            print(f"Successfully downloaded {filename} ({len(b64_str)} base64 chars)")
        else:
            print(f"FAILED to download {filename}")

    ftp.quit()
    print("FTP download complete.")
    
    # Format the JavaScript map string
    js_map = "const EMBEDDED_THUMBNAILS = {\n"
    for name, b64 in embedded_map.items():
        js_map += f'  "{name}": "{b64}",\n'
    js_map += "};\n"
    
    # Read the template and generate final index.js
    template_path = '/root/XPlayGitHub/streamtape-worker/index_template.js'
    dest_path = '/root/XPlayGitHub/streamtape-worker/index.js'
    
    with open(template_path, 'r') as f:
        template = f.read()
        
    final_code = template.replace('// PLACEHOLDER_FOR_BASE64_MAP', js_map)
    
    with open(dest_path, 'w') as f:
        f.write(final_code)
        
    print(f"Successfully generated final index.js at {dest_path}")

if __name__ == '__main__':
    run()
