import { preview } from 'vite'; await preview({ configFile: false, preview: { host: '127.0.0.1', port: 5173, strictPort: true, proxy: { '/api': 'http://localhost:8080' } } });
