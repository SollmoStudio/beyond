exports.query = function () {
    return new Query();
};

exports.Collection = Collection;
exports.Schema = Schema;

exports.ObjectId = function(value) {
    return value ? new ObjectId(value) : new ObjectId();
};
