/* global PluginEvent */
var util = require('util');
importClass(Packages.beyond.engine.javascript.lib.PluginEvent);
var nativeEvent = new PluginEvent();
exports.track = function (tag) {
  var formatString,
      tokenLength,
      tokens;
  tokens = Array.prototype.slice.call(arguments, 1); // arguments[0] is tag.
  tokenLength = tokens.length;
  if (tokenLength !== 0) {
    formatString = tokens.map(function () { return '%j'; }).join(', ');
    if (tokenLength > 1) {
      formatString = '[' + formatString + ']';
    }
    tokens.unshift(formatString);
  }
  var message = util.format.apply(util.format, tokens);
  nativeEvent.track(tag, message);
};
