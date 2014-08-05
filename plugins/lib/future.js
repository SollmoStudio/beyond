exports.successful = function (value) {
    return Future.successful(value);
}

exports.failed = function (message) {
    return new Future(function () { throw message; });
}

exports.create = function (func) {
    return new Future(func);
}
