var util = require('util');

importClass(Packages.beyond.engine.javascript.lib.PluginConsole);
var nativeConsole = new PluginConsole();
exports.log = function () {
    var message = util.format.apply(util.format, arguments);
    nativeConsole.log(message);
}

exports.info = function () {
    var message = util.format.apply(util.format, arguments);
    nativeConsole.info(message);
}

exports.warn = function () {
    var message = util.format.apply(util.format, arguments);
    nativeConsole.warn(message);
}

exports.debug = function () {
    var message = util.format.apply(util.format, arguments);
    nativeConsole.debug(message);
}

exports.error = function () {
    var message = util.format.apply(util.format, arguments);
    nativeConsole.error(message);
}
