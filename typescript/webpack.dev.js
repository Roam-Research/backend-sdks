const HtmlWebpackPlugin = require("html-webpack-plugin");
const TerserPlugin = require("terser-webpack-plugin");
const path = require("path");

module.exports = {
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
    filename: "index.js",
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
};
