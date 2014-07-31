var counter = require("counter");
var db = require("examples/db");

exports.handle = function (req) {
    var tokens = req.uri.split("/");
    tokens.shift();
    tokens.shift();
    var operator = tokens.shift();
    switch (operator) {
        case "counter":
            return new Response('Hello ' + req.uri + ' ' + counter.count());
        case "insert":
            db.insert(tokens[0], tokens[1]);
            break;
        case "find":
            db.find.apply(db.find, tokens);
            break;
        case "findOne":
            db.findOne.apply(db.findOne, tokens);
            break;
        default:
            break;
    }
    return new Response("");
}
