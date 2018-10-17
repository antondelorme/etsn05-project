window.base = window.base || {};

window.base.searchController = (() => {
    let model = {
        user: {},
        searchResults: [],
        searchQuery: {}
    };

    const view = {
        showFailure: msg => {
            document.getElementById('search-results').innerHTML = `<div class="row">\n                        <div class="col-12">\n                            <h5 class="text-muted font-weight-bold">Ooops!</h5>\n                            <p class="text-muted">Something went wrong, error message: ${msg}.</p>\n                        </div>\n                    </div>`;
        },
        renderSearchResults: () => {
            const months = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'];
            const optActive = 'text-info';
            const optNotActive = 'text-muted';
            let searchResultsWrapper = document.getElementById('search-results');
            let searchResults = '';

            for (let i = 0; i < model.searchResults.length; i++) {
                const drive = model.searchResults[i].drive;
                const driveId = drive.driveId;
                const driveName = drive.start + ' to ' + drive.stop;
                const dtd = new Date(drive.departureTime);

                let departureTime = months[dtd.getMonth()] + ' ';
                departureTime += dtd.getDate() + ' at ';
                departureTime += dtd.getHours() + ':' + dtd.getMinutes();

                const carBrand = drive.carBrand;
                const carModel = drive.carModel;
                const carColor = drive.carColor;
                const carLicensePlate = drive.carLicensePlate;
                const carNumberOfSeats = drive.carNumberOfSeats;
                const optLuggage = drive.optLuggageSize > 0 ? optActive : optNotActive;
                const optWinterTires = drive.optWinterTires ? optActive : optNotActive;
                const optBicycle = drive.optBicycle ? optActive : optNotActive;
                const optPets = drive.optPets ? optActive : optNotActive;

                const driver = model.searchResults[i].driver;

                const driverFirstName = driver.firstName;
                const driverGender = driver.gender === 0 ? 'Male' : 'Female';

                const today = new Date();
                const dob = new Date(driver.dateOfBirth);
                let driverAge = today.getFullYear() - dob.getFullYear();
                const m = today.getMonth() - dob.getMonth();
                if (m < 0 || (m === 0 && today.getDate() < dob.getDate())) {
                    driverAge--;
                }

                const driverNumberDriven = driver.numberOfDrives;
                const driverReviews = driver.numberOfRatings;
                const driverRating = driverReviews === 0 ? 'No' : parseFloat(driver.ratingTotalScore / driverReviews).toFixed(2);

                const users = model.searchResults[i].users;
                let confirmedPassengers = 0;
                let requestButtonText = 'View';
                let requestButtonDisabled = '';

                for (let j = 0; j < users.length; j++) {
                    if (users[j].userId === model.user.userId) {
                        requestButtonText = 'Requested';
                        requestButtonDisabled = 'disabled';
                    }

                    if (users[j].userId === model.user.userId && users[j].driver) {
                        requestButtonText = 'Driving';
                    }

                    confirmedPassengers += users[j].accepted ? 1 : 0;
                }

                const driveNumberOfSeatsLeft = carNumberOfSeats - confirmedPassengers;
                let tripStart = model.searchQuery.tripStart;
                tripStart = typeof tripStart === 'undefined' || tripStart == null ? drive.start : tripStart;

                const milestones = model.searchResults[i].milestones;
                milestones.push({milestone: drive.start, departureTime: drive.departureTime});

                let tripStartTime = '';

                for (let j = 0; j < milestones.length; j++) {
                    if (tripStart.toLowerCase().trim() === milestones[j].milestone.toLowerCase().trim()) {
                        const milestoneDepartureTime = new Date(milestones[j].departureTime);
                        tripStartTime += milestoneDepartureTime.getHours() + ':' + milestoneDepartureTime.getMinutes();
                    }
                }

                const tripStartArr = tripStart.split(' ');
                tripStart = '';

                for (let j = 0; j < tripStartArr.length; j++) {
                    tripStart += tripStartArr[j].charAt(0).toUpperCase() + tripStartArr[j].slice(1);
                }

                let tripStop = model.searchQuery.tripStop;
                tripStop = typeof tripStop === 'undefined' || tripStop === null ? drive.stop : tripStop;

                const tripStopArr = tripStop.split(' ');
                tripStop = '';

                for (let j = 0; j < tripStopArr.length; j++) {
                    tripStop += tripStopArr[j].charAt(0).toUpperCase() + tripStopArr[j].slice(1);
                }

                let passengerIcons = '';

                for (let i = carNumberOfSeats; i > 0; i--) {
                    passengerIcons += i <= driveNumberOfSeatsLeft ?
                        '<i class="fas fa-user-check text-muted"/>' :
                        '<i class="fas fa-user-check text-info"/>'
                }

                // language=HTML
                searchResults += `\n        <div class="row mb-3 border bg-white shadow-sm">\n            <div class="col-2 border-right">\n                <h5 class="mt-3 mb-0 text-muted font-weight-bold">${driverFirstName}</h5>\n                <p class="text-muted">${driverGender}, ${driverAge}</p>\n                <div class="row mt-4">\n                    <div class="col-2">\n                        <p class="mb-0 text-muted"><i class="fas fa-car fa-sm"></i></p>\n                        <p class="text-muted mb-0"><i class="fas fa-star fa-sm"></i></p>\n                        <p class="text-muted"><i class="fas fa-chart-bar fa-sm"></i></p>\n                    </div>\n                    <div class="col-9">\n                        <p class="text-muted mb-0">${driverNumberDriven} driven</p>\n                        <p class="text-muted mb-0">${driverRating} rating</p>\n                        <p class="text-muted">${driverReviews} reviews</p>\n                    </div>\n                </div>\n            </div>\n            <div class="col-5 border-right">\n                <table class="h-100 w-100">\n                    <tr>\n                        <td class="align-top">\n                            <a class="drive-link" id="drive-${driveId}" href="">\n                                <h5 class="mt-3 mb-0 text-danger font-weight-bold">${driveName}</h5>     \n                            </a>\n                            <div class="row">\n                                <div class="col-3">\n                                        <p class="text-muted">Leaving:</p>\n                                </div>\n                                <div class="col-9">\n                                    <p class="text-muted">${departureTime}</p>\n                                </div>\n                            </div>\n                        </td>\n                    </tr>\n                    <tr>\n                        <td class="align-bottom">\n                            <div class="row mb-0">\n                                <div class="col-3">\n                                    <p class="text-muted mb-0">Pickup:</p>\n                                    <p class="text-muted">Dropoff:</p>\n                                </div>\n                                <div class="col-9">\n                                    <p class="text-muted mb-0"><span>${tripStart}</span> (~<span>${tripStartTime}</span>)</p>\n                                    <p class="text-muted">${tripStop}</p>\n                                </div>\n                            </div>\n                        </td>\n                    </tr>\n                </table>\n            </div>\n            <div class="col-3 border-right">\n                <table class="h-100 w-100">\n                    <tr>\n                        <td class="align-top">\n                            <h5 class="mt-3 mb-0 text-muted font-weight-bold">${carBrand} ${carModel}</h5>\n                            <p class="text-muted mb-0">${carColor} | ${carLicensePlate}</p>\n                        </td>\n                    </tr>\n                    <td class="align-bottom">\n                        <p class="text-muted mb-0">Vehicle preferences</p>\n                        <p>\n                            <i class="fas fa-suitcase fa-lg ${optLuggage}"></i>\n                            <i class="fas fa-snowflake fa-lg ${optWinterTires}"></i>\n                            <i class="fas fa-bicycle fa-lg ${optBicycle}"></i>\n                            <i class="fas fa-paw fa-lg ${optPets}"></i>\n                        </p>\n                    </td>\n                </table>\n            </div>\n            <div class="col-2">\n                <table class="h-100 w-100">\n                    <tr>\n                        <td class="align-top">\n                            <h5 id="drive-seats-left" class="mt-3 text-muted font-weight-bold">${driveNumberOfSeatsLeft} seats left</h5>\n                            ${passengerIcons}\n                        </td>\n                    </tr>\n                    <td class="align-bottom">\n                        <button class="mb-3 btn btn-danger btn-sm btn-block request-btn" ${requestButtonDisabled}>${requestButtonText}</button>\n                    </td>\n                </table>\n            </div>\n        </div>`

            }

            if (searchResults.length === 0) {
                searchResults = `<div class="row">\n                        <div class="col-12">\n                            <h5 class="text-muted font-weight-bold">No trips found</h5>\n                        </div>\n                    </div>`
            }

            searchResultsWrapper.innerHTML = searchResults;
            const driveLinks = document.getElementsByClassName('drive-link');
            const requestButtons = document.getElementsByClassName('request-btn');

            for (let i = 0; i < driveLinks.length; i++) {
                const link = driveLinks[i];
                const requestButton = requestButtons[i];

                link.onclick = e => {
                    e.preventDefault();
                    model.searchQuery.driveId = link.id.split('-')[1];
                    const milestones = link
                        .parentElement.parentElement
                        .nextElementSibling
                        .children[0].children[0].children[1].children;
                    model.searchQuery.tripStart = milestones[0].children[0].textContent;
                    model.searchQuery.tripStartTime = milestones[0].children[1].textContent;
                    model.searchQuery.tripStop = milestones[1].textContent;
                    controller.goToDrive();
                };

                requestButton.onclick = e => {
                    e.preventDefault();
                    link.click();
                };
            }
        }
    };

    const controller = {
        sanitizeDateTimeInput: field => {
            let input = field.value;
            input = input.replace(/[^\d]+/g, '');
            input = input.length > 12 ? input.slice(0, 12) : input;

            if (input.length > 4 && input.indexOf('-') !== 4) {
                input = input.slice(0, 4) + '-' + input.slice(4);
            }

            if (input.length > 7 && input.lastIndexOf('-') !== 6) {
                input = input.slice(0, 7) + '-' + input.slice(7);
            }

            if (input.length > 10 && input.lastIndexOf(' ') !== 9) {
                input = input.slice(0, 10) + ' ' + input.slice(10);
            }

            if (input.length > 13 && input.lastIndexOf(':') !== 12) {
                input = input.slice(0, 13) + ':' + input.slice(13);
            }

            field.value = input;
        },
        submitSearch: e => {
            e.preventDefault();

            let start = document.getElementById('search-start').value.trim();
            start = start.length === 0 ? null : start;

            let stop = document.getElementById('search-stop').value.trim();
            stop = stop.length === 0 ? null : stop;

            let departureTime = document.getElementById('search-departure-time').value.trim();
            departureTime = departureTime.length === 0 ? -1 : new Date(departureTime).getTime();

            model.searchQuery = {tripStart: start, tripStop: stop, tripDepartureTime: departureTime};

            model.searchResults = [];

            controller.performSearch({
                searchFilterId: 0,
                userId: model.user.userId,
                start,
                stop,
                departureTime
            })
                .then(() => view.renderSearchResults())
                .catch(e => view.showFailure(e));
        },
        getDriverInfo: (userId, driveIndex) => window.base.rest.getUser(userId)
            .then(driver => model.searchResults[driveIndex].driver = driver),
        getNumberOfDrives: (userId, driveIndex) => window.base.rest.getNumberOfDrivesForUser(userId)
            .then(i => model.searchResults[driveIndex].driver.numberOfDrives = i),
        performSearch: searchFilter => window.base.rest.searchDrives(searchFilter)
            .then(drives => {
                if (drives.error) throw drives;

                // Assign results to model
                model.searchResults = drives;

                // Loop through and get driver info
                return Promise.all(drives.map((drive, driveIndex) => {
                    const users = model.searchResults[driveIndex].users;

                    for (let j = 0; j < users.length; j++) {
                        if (users[j].driver) {
                            const userId = users[j].userId;
                            return controller.getDriverInfo(userId, driveIndex)
                                .then(() => controller.getNumberOfDrives(userId, driveIndex));
                        }
                    }
                }));
            }),
        getUser: () => window.base.rest.getUser().then(user => model.user = user),
        goToDrive: () => {
            fetch('templates/drive.html')
                .then(response => response.text())
                .then(tabHtml => {
                    document.getElementById('main-tab').innerHTML = tabHtml;
                    window.base.driveController().loadQuery(model.searchQuery);
                })
        },
        load: () => {
            document.getElementById('drive-search').onsubmit = controller.submitSearch;
            const departureTime = document.getElementById('search-departure-time');

            departureTime.addEventListener('focusin', () => {
                departureTime.setAttribute('placeholder', 'YYYY-MM-DD HH:MM');
            });

            departureTime.addEventListener('focusout', () => {
                departureTime.setAttribute('placeholder', 'Leaving');
            });

            departureTime.onkeyup = () => {
                controller.sanitizeDateTimeInput(departureTime);
            };

            controller.getUser()
                .then(() => controller.performSearch({
                    searchFilterId: 0,
                    userId: model.user.userId,
                    start: null,
                    stop: null,
                    departureTime: -1
                }))
                .then(() => view.renderSearchResults())
                .catch(e => view.showFailure(e.message));
        },
        initOnLoad: () => document.addEventListener('DOMContentLoaded', window.base.searchController.load())
    };

    return controller;
});