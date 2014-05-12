var counter = require("counter");

exports.handle = function(req) {
    return 'Hello ' + req.uri + ' ' + counter.count();
};

