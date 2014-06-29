exports.successful = function (value) {
    return new Future(function () { return value; });
}

exports.failed = function (message) {
    return new Future(function () { throw message; });
}

exports.create = function (func) {
    return new Future(func);
}
