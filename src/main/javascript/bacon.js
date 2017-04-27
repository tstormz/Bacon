function Bacon(domObject) {
    var NODE_WIDTH = 100;
    var NODE_HEIGHT = 50;
    var PADDING = 20;
    var NODE_COLOR = "#333";
    var HOST = "http://13.58.74.138:8080/v1/";

    var previousCoordinate = {x: -1, y: -1};
    var currentCoordinate = {x: -1, y: -1};

    var actorId = 0;
    var movieId = 0;

    var actor1, actor2;

    var canProceed = true;

    /**
     * Draws a new node using the coordinates of a mouse click
     */
    this.createNode = function(x, y) {
        if (canProceed) {
            var midX = x - (NODE_WIDTH >> 1);
            var midY = y - (NODE_HEIGHT >> 1);
            var ctx = context();
            ctx.fillStyle = NODE_COLOR;
            // draw the node background
            ctx.fillRect(midX, midY, NODE_WIDTH, NODE_HEIGHT);
            // create a textfield for the actor
            createInput("actor", x - (NODE_WIDTH >> 2), y);
            if (currentCoordinate.x === -1) {
                currentCoordinate.x = x;
                currentCoordinate.y = y;
            } else {
                saveCoordinates(x, y);
                connectNodes();
                // create text field for the movie
                var movieInputX = findMidPoint(previousCoordinate.x, currentCoordinate.x);
                var movieInputY = findMidPoint(previousCoordinate.y, currentCoordinate.y);
                createInput("movie", movieInputX, movieInputY);
                canProceed = false;
            }
        }
    };

    /**
     * Creates a text input for the user to input an actor or movie
     */
    function createInput(type, x, y) {
        var textField = document.createElement("input");
        textField.type = "text";
        if (type === "actor") {
            actorId += 1;
            textField.id = type + actorId;
            addActorAction(textField, actorId % 2 === 0);
        } else if (type === "movie") {
            movieId += 1;
            textField.id = type + movieId;
            addMovieAction(textField);
        }
        textField.style.position = "absolute";
        textField.style.left = x + "px";
        textField.style.top = y + "px";
        textField.style.width = NODE_WIDTH - PADDING * 2 + "px";
        textField.style.zIndex = "1";
        document.getElementById("inputLayer").appendChild(textField);
        document.getElementById("actor" + actorId).focus();
    }

    /**
     * Stores the new coordinates, and a copy of the previous coordinates for use in drawing the next edge
     */
    function saveCoordinates(x, y) {
        previousCoordinate.x = currentCoordinate.x;
        previousCoordinate.y = currentCoordinate.y;
        currentCoordinate.x = x;
        currentCoordinate.y = y;
    }

    /**
     *  Draws a line between the previous created node and the current node
     */
    function connectNodes() {
        var ctx = context();
        ctx.strokeStyle = NODE_COLOR;
        ctx.lineWidth = 5;
        ctx.moveTo(previousCoordinate.x, previousCoordinate.y);
        ctx.lineTo(currentCoordinate.x, currentCoordinate.y);
        ctx.stroke();
    }

    /**
     * Finds the mid point in terms of pixels. Used for placing the
     * movie input
     */
    function findMidPoint(previous, current) {
        var length;
        if (previous > current) {
            length = previous - current;
            return previous - (length >> 1);
        } else {
            length = current - previous;
            return previous + (length >> 1);
        }
    }

    function addActorAction(textField, oddOrEven) {
        textField.addEventListener("keyup", function(e) {
            e.preventDefault();
            if (e.keyCode === 13) {
                var url = HOST + "actors/" + format(textField.value);
                var xhr = createCORSRequest('GET', url);
                xhr.onload = function() {
                    if (JSON.parse(xhr.responseText).length !== 0) {
                        if (oddOrEven) {
                            actor1 = new Actor(xhr.responseText);
                            actor1.print("debug");
                        } else {
                            actor2 = new Actor(xhr.responseText);
                            actor2.print("debug");
                        }
                        textField.style.border = "2px solid green";
                    } else {
                        document.getElementById("debug").innerHTML = "error: no actor by that name found";
                    }
                };
                xhr.send();
                if (movieId > 0) {
                    document.getElementById("movie" + movieId).focus();
                }
            }
        });
    }

    function addMovieAction(textField) {
        textField.addEventListener("keyup", function(e) {
            e.preventDefault();
            if (e.keyCode === 13) {
                var url = HOST + "movies/" + format(textField.value);
                var xhr = createCORSRequest('GET', url);
                xhr.onload = function() {
                    var movieData = JSON.parse(xhr.responseText);
                    if (movieData.length !== 0) {
                        for (var i = 0; i < movieData.length; i++) {
                            movie = new Movie(movieData[i]);
                            movie.print("debug");
                            canProceed = movie.verifyActors(actor1.id, actor2.id);
                            if (canProceed) {
                                textField.style.border = "2px solid green";
                                break;
                            }
                        }
                        if (!canProceed) {
                            alert("Both " + actor1.name + " and " + actor2.name + " must be cast in " + movie.title);
                        }
                    }
                };
                xhr.send();
            }
        });
    }

    function createCORSRequest(method, url) {
        var xhr = new XMLHttpRequest();
        if ("withCredentials" in xhr) {
            // Check if the XMLHttpRequest object has a "withCredentials" property.
            // "withCredentials" only exists on XMLHTTPRequest2 objects.
            xhr.open(method, url, true);
        } else if (typeof XDomainRequest !== "undefined") {
            // Otherwise, check if XDomainRequest.
            // XDomainRequest only exists in IE, and is IE's way of making CORS requests.
            xhr = new XDomainRequest();
            xhr.open(method, url);
        } else {
            // Otherwise, CORS is not supported by the browser.
            xhr = null;
        }
        return xhr;
    }

    function format(str) {
        var words = str.split(" ");
        var i = 0;
        var formatted = "";
        for (; i < words.length; i++) {
            formatted += "_" + words[i];
        }
        return formatted.substring(1);
    }

    /**
     * Returns the canvas context
     */
    function context() {
        return document.getElementById(domObject).getContext("2d");
    }
}
