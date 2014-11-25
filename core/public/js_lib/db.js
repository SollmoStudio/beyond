/* global Collection, ObjectId, Query, Schema */
exports.query = function () {
    if (arguments.length === 2) {
        return new Query(arguments[0], arguments[1]);
    } else {
        return new Query();
    }
};

exports.Collection = Collection;
exports.Schema = Schema;

exports.ObjectId = function(value) {
    return value ? new ObjectId(value) : new ObjectId();
};

exports.ASC = 1;
exports.DESC = -1;
