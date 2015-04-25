exports.successful = Future.successful;

exports.sequence = Future.sequence;

exports.firstCompletedOf = Future.firstCompletedOf;

exports.failed = function (message) {
  return new Future(function () { throw message; });
};

exports.create = function (func) {
  return new Future(func);
};
