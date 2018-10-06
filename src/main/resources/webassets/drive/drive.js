var base = base || {};
base.driveController = (function() {
    var controller = {
        load: function() {
            
            base.Drive = 
            base.rest.putDrive(id, drive).then(function(response) {
                console.log("Driver added");
            }),
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