/** @jsx React.DOM */
define(function() {
    var Table = React.createClass({
        render: function() {
            var tableRows = $.map(this.props.data, function(value, key) {
                return <tr><td>{key}</td><td>{value}</td></tr>;
            });

            return (
                <table className="table">
                            {tableRows}
                </table>
                );
        }
    });

    var PanelBody = React.createClass({
        render: function() {
            return (
                <div className="panel-body">
                    <p>{this.props.children}</p>
                </div>
                );
        }
    });

    var PanelHeading = React.createClass({
        render: function() {
            return (
                <div className="panel-heading">{this.props.heading}</div>
                );
        }
    });

    var TablePanel = React.createClass({
        render: function() {
            return (
                <div className="panel panel-default">
                    <PanelHeading heading={this.props.heading} />
                    <PanelBody>{this.props.children}</PanelBody>
                    <Table data={this.props.data} />
                </div>
                );
        }
    });

    return TablePanel;
});
