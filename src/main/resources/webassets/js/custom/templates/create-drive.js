window.base = window.base || {};

window.base.createDriveController = (() => {
    const model = {
        driveWraps: {},
        user: {},
        theId: {}
    };

    const view = {
        render: () => window.base.rest.getUser().then(u => {
            model.user = u;
        }).then(u => {
            window.base.rest.getDrivesForUser(model.user.userId).then(d => {
                model.driveWraps = d;
                if (d.length === 0 || model.theId.id === undefined) {
                    document.getElementById('drive-header').innerHTML = 'Create Drive';
                    document.getElementById('passenger-header').remove();
                    document.getElementById('passenger-col').remove();
                } else {
                    document.getElementById('drive-header').innerHTML = 'Edit Drive';
                    for (var i = 0; i < d.length; i++) {
                        if (d[i].drive.driveId === model.theId.id) {
                            model.driveWrap = d[i];
                        }
                    }
                }
            }).then(() => {
                if (model.driveWraps.length === 0 || model.theId.id === undefined) {
                }else {
                    controller.setInput();
                }
            });
        })
    };

    const controller = {
        setInput: () => {

            Date.prototype.yyyymmdd = function() {
                var mm = this.getMonth() + 1; // getMonth() is zero-based
                var dd = this.getDate();

                return [this.getFullYear(),
                        (mm>9 ? '' : '0') + '-' + mm,
                        (dd>9 ? '' : '0') + '-' + dd
                       ].join('');
            };
            document.getElementById('set-from').value = model.driveWrap.drive.start;
            document.getElementById('set-to').value = model.driveWrap.drive.stop;
            const leaveDate = new Date(model.driveWrap.drive.departureTime);
            const arrivalDate = new Date(model.driveWrap.drive.arrivalTime);
            document.getElementById('set-date').value = leaveDate.yyyymmdd();
            document.getElementById('set-time-leave').value = leaveDate.toString().split(' ')[4];
            document.getElementById('set-time-arrival').value = arrivalDate.toString().split(' ')[4];
            document.getElementById('set-comment').value = model.driveWrap.drive.comment;
            document.getElementById('set-brand').value = model.driveWrap.drive.carBrand;
            document.getElementById('set-model').value = model.driveWrap.drive.carModel;
            document.getElementById('set-color').value = model.driveWrap.drive.color;
            document.getElementById('set-licence').value = model.driveWrap.drive.carLicensePlate;
            document.getElementById('set-seats').value = model.driveWrap.drive.carNumberOfSeats;
            if (model.driveWrap.drive.optWinterTires === true) {
                document.getElementById('set-tires').checked = true;
            } else {
                document.getElementById('set-tires').checked = false;
            }
            if (model.driveWrap.drive.optBicycle === true) {
                document.getElementById('set-bikes').checked = true;
            } else {
                document.getElementById('set-bikes').checked = false;
            }
            if (model.driveWrap.drive.optPets === true) {
                document.getElementById('set-pets').checked = true;
            } else {
                document.getElementById('set-pets').checked = false;
            }
            if (model.driveWrap.drive.optLuggageSize === 0) {
                document.getElementById('set-luggage').options[0].selected = true;
            } else if (model.driveWrap.drive.optLuggageSize === 1) {
                document.getElementById('set-luggage').options[1].selected = true;
            } else {
                document.getElementById('set-luggage').options[2].selected = true;
            }
            const stopMilestones = model.driveWrap.milestones;
            for(var i = 0; i < stopMilestones.length; i++){
                controller.addStop(stopMilestones[i].milestone);
            }

            //Get passengers for drive
            const nbrPassengers = model.driveWrap.users.length;
            for(var i = 1; i < nbrPassengers; i++) {
                const userId = model.driveWrap.users[i].userId;
                const nameCol = document.createElement('div');
                const nameText = document.createElement('p');
                nameCol.className = 'col-6 mt-3';
                nameCol.id = 'remove-col-' + i;
                window.base.rest.getUser(model.driveWrap.users[i].userId).then(u => {
                    nameText.innerHTML = u.firstName;
                });
                const removeCol = document.createElement('div');
                const removeBtn = document.createElement('button');
                removeCol.className = 'col-6 mt-3';
                removeCol.id = 'remove-col-' + i;
                removeBtn.type = 'button';
                removeBtn.className = 'btn btn-danger w-100';
                removeBtn.innerHTML = 'Remove';
                removeBtn.id = 'removePass-' + i;
                (function(i){
                    removeBtn.onclick = (function (callback) {
                        //Vad ska göras när man klickar på remove user?
                        document.getElementById('remove-col-' + i).remove();
                        document.getElementById('remove-col-' + i).remove();
                        window.base.rest.removeUserFromDrive(model.driveWrap.drive.driveId, userId);
                    });
                })(i);
                nameCol.append(nameText);
                removeCol.append(removeBtn);
                document.getElementById('passenger-row').append(nameCol);
                document.getElementById('passenger-row').append(removeCol);
                incrementer++;
            }
        },

        removeStop: () => {
            const counter = document.querySelectorAll("#stop-row .stop-div").length;
            if (counter > 0){
                document.getElementById('stop-' + counter).remove();
            }
        },

        addStop: (text) => {
            if (typeof(text) == 'object'){
                const counter = document.querySelectorAll("#stop-row .stop-div").length;
                const stopDiv = document.createElement('div');
                const col = document.createElement('div')
                const colMd = document.createElement('div');
                col.className = 'col';
                colMd.className = 'col-md';
                stopDiv.className = 'row mt-2 stop-div';
                stopDiv.id = 'stop-' + (counter + 1);
                const textArea = document.createElement('textarea');
                const label = document.createElement('label');
                label.innerHTML = 'Stop ' + (counter + 1);
                textArea.id = 'set-stop-' + (counter+1);
                textArea.className = 'form-control';
                colMd.append(label);
                colMd.appendChild(textArea);
                col.append(colMd);
                stopDiv.append(col);
                document.getElementById('stop-row').append(stopDiv);
                textArea.innerHTML = '';
            } else {
                const counter = document.querySelectorAll("#stop-row .stop-div").length;
                const stopDiv = document.createElement('div');
                const col = document.createElement('div')
                const colMd = document.createElement('div');
                col.className = 'col';
                colMd.className = 'col-md';
                stopDiv.className = 'row mt-2 stop-div';
                stopDiv.id = 'stop-' + (counter + 1);
                const textArea = document.createElement('textarea');
                const label = document.createElement('label');
                label.innerHTML = 'Stop ' + (counter + 1);
                textArea.id = 'set-stop-' + (counter+1);
                textArea.className = 'form-control';
                colMd.append(label);
                colMd.appendChild(textArea);
                col.append(colMd);
                stopDiv.append(col);
                document.getElementById('stop-row').append(stopDiv);
                textArea.innerHTML = text;
            }
        },

        createDrive: () => {
            let optWinterTires;
            let optBicycle;
            let optPets;
            const start = document.getElementById('set-from').value;
            const stop = document.getElementById('set-to').value;
            const date = document.getElementById('set-date').value;
            const arrivalValue = document.getElementById('set-time-arrival').value;
            const arrivalDate = new Date(date + "T" + arrivalValue + ":00+0200");
            const arrivalTime = arrivalDate.getTime();
            const comment = document.getElementById('set-comment').value;
            const carBrand = document.getElementById('set-brand').value;
            const carModel = document.getElementById('set-model').value;
            const departureValue = document.getElementById('set-time-leave').value;
            const departureDate = new Date(date + "T" + departureValue + ":00+0200");
            const departureTime = departureDate.getTime();
            const carColor = document.getElementById('set-color').value;
            const carLicensePlate = document.getElementById('set-licence').value;
            const carNumberOfSeats = document.getElementById('set-seats').value;
            const tmpLuggage = document.getElementById('set-luggage');
            const optLuggageSize = tmpLuggage.options[tmpLuggage.selectedIndex].value;

            if (document.getElementById('set-tires').checked) {
                optWinterTires = true;
            } else {
                optWinterTires = false;
            };
            if (document.getElementById('set-bikes').checked) {
                optBicycle = true;
            } else {
                optBicycle = false;
            };
            if (document.getElementById('set-pets').checked) {
                optPets = true;
            } else {
                optPets = false;
            };

            const driveId = 0;

            const drive = {driveId, start, stop, departureTime, arrivalTime, comment, carBrand, carModel, carColor, carLicensePlate, carNumberOfSeats, optLuggageSize, optWinterTires, optBicycle, optPets};

            const milestones = [];
            let driveMilestone;

            for (var i = 1; i <= document.querySelectorAll("#stop-row .stop-div").length; i++) {
                const milestoneId = i;
                const milestone = document.getElementById('set-stop-' + i).value;
                driveMilestone = {milestoneId, driveId, milestone, departureTime};
                milestones.push(driveMilestone);
            }

            const user = model.user;
            const users = [user];

            const reports = [];

            const driveWrap = {drive, milestones, users, reports};

            window.base.rest.addDrive(driveWrap).then(d => {
                if (d.error) {
                    alert(d.error);
                }
            }).then(() => {
                view.render(model.theId.id);
                alert('Drive has been created');
            });
        },

        updateDrive: () => window.base.rest.updateDrive(),

        deleteDrive: () => window.base.rest.deleteDrive(model.driveWrap.drive.driveId)
        .then(() => window.location.replace('/')),

        load: (id) => {
            model.theId.id = id;
            document.getElementById('user-form').onsubmit = controller.createDrive;
            document.getElementById('delete-drive-btn').onclick = controller.deleteDrive;
            document.getElementById('add-stop-btn').onclick = controller.addStop;
            document.getElementById('remove-stop-btn').onclick = controller.removeStop;
            view.render(id);
        },

        loadWithUserId: (id) => {
            controller.load(id);
        },
    };

    return controller;
});