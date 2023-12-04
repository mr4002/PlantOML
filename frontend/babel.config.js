module.exports = (api) => {
    api.cache(false);

    return {
        "presets": ["@babel/preset-react"],
        plugins: [
            "@babel/plugin-transform-modules-commonjs",
            "babel-plugin-inline-react-svg"
        ]
    };
};
