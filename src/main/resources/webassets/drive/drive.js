var base = base || {};
base.driveController = (function() {
    var controller = {
        load: function() {
            base.rest.getDrive().then(function(response) {
                console.log(response);
            })
        },
        initOnLoad: function() {
            document.addEventListener('DOMContentLoaded', base.driveController.load);
        }
    };
    return controller;
});