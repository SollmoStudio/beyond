/** @jsx React.DOM */
define(["jquery", "react", "bootstrap"], function ($, React) {
    return React.createClass({
        render: function() {
            var columns = this.props.columns;
            var tableHeadRows = $.map(columns, function (value) {
                return <th>{value.name}</th>;
            });
            var tableRows = $.map(this.props.data, function (value) {
                var tableRowDatas = $.map(columns, function (column) {
                    return <td>{value[column.key]}</td>;
                });
                return <tr>{tableRowDatas}</tr>;
            });

            return (
                <div className="table-responsive">
                    <table className="table table-stripped">
                        <thead>
                            {tableHeadRows}
                        </thead>
                        <tbody>
                            {tableRows}
                        </tbody>
                    </table>
                </div>
            );
        }
    });
});
