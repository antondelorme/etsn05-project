
var base = base || {};
base.searchController = (function() {
    var controller = {
        load: function() {
            console.log("Hello!");
        },
        initOnLoad: function() {
            document.addEventListener('DOMContentLoaded', base.searchController.load);
        }
    };

    return controller;
});