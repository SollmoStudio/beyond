/** @jsx React.DOM */
define(["jquery", "react", "bootstrap"], function ($, React) {
    return React.createClass({
        getHashTag: function () {
            // TODO
            return "systemLoadAverage";
        },
        getInitialState: function () {
            return {data: {}};
        },
        componentDidMount: function () {
            // TODO
            document.onhashchange(this.updateTable);
            this.updateTable();
        },
        updateTable: function () {
            var that = this;
            var tableName = this.getHashTag();
            $.ajax({
                url: '?',
                dataType: 'json',
                success: function (data) {
                    that.setState({data: data});
                }
            });
        },
        render: function() {
            // this.states.data.header
            // this.states.data.column
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
                <div>
                    <h1 class="page-header">{this.states.data.header}</h1>
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
                </div>
            );
        }
    });
});
