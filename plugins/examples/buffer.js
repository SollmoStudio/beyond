var Buffer = require("buffer").Buffer;

exports.test = function () {
  var result = {};

  result.newWithSize = JSON.stringify(new Buffer(10));
  result.newWithArray = JSON.stringify(new Buffer([10, 20, 30, 40, 50]));
  result.newWithString = JSON.stringify(new Buffer("Hello, world!", 'UTF-8'));

  result.isSupportedEncoding = Buffer.isSupportedEncoding('UTF-8'); // true

  var buf = new Buffer(128);
  result.length = buf.length; // 128

  var length = buf.write("Hello, write!", 3, 5, 'UTF-8');
  result.writeAndToString = buf.toString("UTF-8", 3, 3 + length); // 'Hello'

  result.byteLength = Buffer.byteLength("Hello, byteLength!", 'UTF-8'); // 18

  var buf1 = new Buffer("Hell", 'UTF-8');
  var buf2 = new Buffer("o, w", 'UTF-8');
  var buf3 = new Buffer("orld", 'UTF-8');
  var buf4 = new Buffer("!...", 'UTF-8');
  result.concat = Buffer.concat([buf1, buf2, buf3, buf4], 13).toString(); // 'Hello, world!'

  var buf5 = new Buffer(10);
  buf5.fill(1);
  buf5.fill(22, 3, 8);
  result.fill = JSON.stringify(buf5); // [1,1,1,22,22,22,22,22,1,1]

  var buf6 = new Buffer("Hallo, world!");
  var buf7 = buf6.slice(1, 6);
  result.slice1 = buf7.toString(); // 'allo,'
  result.sliceLByte = buf7[2];
  buf7[0] += 4; // a -> e
  result.slice2 = buf6.toString(); // 'Hello, world!'

  var buf8 = new Buffer("Hello, Seoul!");
  buf6.copyTo(buf8, 7, 7, 12); // copy 'world' replacing 'Seoul'
  result.copyTo = buf8.toString(); // 'Hello, world!'

  return result;
};
