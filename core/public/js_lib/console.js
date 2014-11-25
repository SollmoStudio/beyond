/* global Console */
var util = require('util');

exports.log = function () {
    var message = util.format.apply(util.format, arguments);
    Console.log(message);
};

exports.info = function () {
    var message = util.format.apply(util.format, arguments);
    Console.info(message);
};

exports.warn = function () {
    var message = util.format.apply(util.format, arguments);
    Console.warn(message);
};

exports.debug = function () {
    var message = util.format.apply(util.format, arguments);
    Console.debug(message);
};

exports.error = function () {
    var message = util.format.apply(util.format, arguments);
    Console.error(message);
};
