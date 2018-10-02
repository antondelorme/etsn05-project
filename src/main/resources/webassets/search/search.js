var base = base || {};
base.searchController = (function() {
    var controller = {
        load: function() {
            base.rest.getDrives().then(function(response) {
                console.log(JSON.stringify(response) + "hej");
            });
        },
        initOnLoad: function() {
            document.addEventListener('DOMContentLoaded', base.searchController.load);
        }
    };
    return controller;
});