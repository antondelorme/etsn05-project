var base = base || {};
base.driveController = (function() {
    var controller = {
        load: function() {
           console.log('hej');
        },
        initOnLoad: function() {
            document.addEventListener('DOMContentLoaded', base.driveController.load);
        }
    };
    return controller;
});