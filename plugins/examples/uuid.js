var uuid = require('uuid');

exports.test = function () {
  var result = {};

  result.v1 = uuid.v1();
  result.v1WithOptions = uuid.v1({
    node: 0x0123456789ab,
    clockseq: 0x1234,
    msecs: new Date('2014-01-01').getTime(),
    nsecs: 5678
  });
  result.v4 = uuid.v4();

  return result;
};
