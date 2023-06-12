import HtmlWebpackPlugin from "html-webpack-plugin";
import TerserPlugin from "terser-webpack-plugin";
import path from "path";
import url from "url";

const __dirname = url.fileURLToPath(new URL('.', import.meta.url));

export default {
  optimization: {
 //   minimizer: [new TerserPlugin()],
  },
  entry: "./src/index.ts",
  module: {
    rules: [
      {
        test: /\.tsx?$/,
        use: "ts-loader",
        exclude: /node_modules/,
      },
    ],
  },
  resolve: {
    extensions: [".tsx", ".ts", ".js"],
  },
  output: {
    library: {type: "module"},
    filename: "roamapisdk.js",
    path: path.resolve(__dirname, "dist"),
  },
  plugins: [
  ],
  experiments: {
    outputModule: true,
  },
};
