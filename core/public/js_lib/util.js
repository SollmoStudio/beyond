exports.format = function (inputString) {
  var extraTokens,
      index,
      middleStage,
      pattern,
      tokens;
  tokens = Array.prototype.slice.call(arguments, 1); // arguments[0] is inputString.
  index = 0;
  pattern = /%[sdj%]/g;
  middleStage = String(inputString).replace(pattern, function (placeholder) {
    if (index >= tokens.length) {
      return placeholder;
    }
    switch (placeholder) {
      case '%s':
        return String(tokens[index++]);
      case '%d':
        return Number(tokens[index++]);
      case '%j':
        try {
          return JSON.stringify(tokens[index++]);
        } catch (ex) {
          return '...';
        }
        break;
      case '%%':
        return '%';
      default:
        return placeholder;
    }
  });
  extraTokens = tokens.slice(index);
  extraTokens.unshift(middleStage);
  return extraTokens.join(' ');
};
