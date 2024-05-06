import HtmlWebpackPlugin from "html-webpack-plugin";
import TerserPlugin from "terser-webpack-plugin";
import path from "path";
import url from "url";

const __dirname = url.fileURLToPath(new URL('.', import.meta.url));

export default {
  optimization: {
    minimizer: [new TerserPlugin()],
  },
  entry: "./src/index.ts",
  devtool: "eval-source-map",
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
  devServer: {
    static: path.join(__dirname, "dist"),
    compress: true,
    port: 4000,
    allowedHosts: "all",
  },
  plugins: [
    new HtmlWebpackPlugin({
      title: "Roam Backend plugin",
      template: "src/custom.html",
    }),
  ],
  experiments: {
    outputModule: true,
  }
};
