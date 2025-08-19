import path from "path";
import { defineConfig, splitVendorChunkPlugin } from "vite";

const isDev = process.env.NODE_ENV !== "production";
const webpageOutDir = path.resolve(__dirname, 'webpage', 'target', 'scala-3.7.1', (isDev ? 'webpage-fastopt' : 'webpage-opt'))

export default defineConfig({
  root: path.resolve(__dirname, "webpage"),
  plugins: [splitVendorChunkPlugin()],
  build: {
    outDir: path.resolve(
      __dirname,
      "server",
      "target",
      "scala-3.7.1",
      "resource_managed",
      "main"
    ),
    emptyOutDir: true,
  },
  resolve: {
    alias: [{ find: "@webpage", replacement: webpageOutDir }],
  },
  assetsInclude: ["**/*.wasm?url", "**/*.scm"],
  server: {
    proxy: {
      "/login": { target: "http://localhost:8080", ws: true },
      "/logout": { target: "http://localhost:8080", ws: true },
      "/play": { target: "http://localhost:8080", ws: true },
      "/api": { target: "http://localhost:8080" },
    },
    port: 9000,
    strictPort: true,
  },
});
