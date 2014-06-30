var util = require('util');

var console = new Console();
exports.log = function () {
    var message = util.format.apply(util.format, arguments);
    console.log(message);
}

exports.info = function () {
    var message = util.format.apply(util.format, arguments);
    console.info(message);
}

exports.warn = function () {
    var message = util.format.apply(util.format, arguments);
    console.warn(message);
}

exports.debug = function () {
    var message = util.format.apply(util.format, arguments);
    console.debug(message);
}

exports.error = function () {
    var message = util.format.apply(util.format, arguments);
    console.error(message);
}
