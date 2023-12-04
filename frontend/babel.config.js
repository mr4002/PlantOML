module.exports = (api) => {
    const presets = ["react-app"];
    const plugins = [
        "@babel/plugin-transform-modules-commonjs",
        "babel-plugin-inline-react-svg",
//        "babel-preset-react",
//        "inline-react-svg"
    ]; 
  
    api.cache(false); 
   
    return {
        presets,
        plugins
    };
};
