function Bacon(domObject) {
    var NODE_WIDTH = 100;
    var NODE_HEIGHT = 50;
    var PADDING = 20;
    var NODE_COLOR = "#333";

    var previousCoordinate = {x: -1, y: -1};
    var currentCoordinate = {x: -1, y: -1};

    var actorId = 0;
    var movieId = 0;

    /**
     * Draws a new node using the coordinates of a mouse click
     */
    this.createNode = function(x, y) {
        var midX = x - (NODE_WIDTH >> 1);
        var midY = y - (NODE_HEIGHT >> 1);
        var ctx = context();
        ctx.fillStyle = NODE_COLOR;
        // draw the node background
        ctx.fillRect(midX, midY, NODE_WIDTH, NODE_HEIGHT);
        // create a textfield for the actor
        createInput("actor", x - (NODE_WIDTH >> 2), y);
        if (currentCoordinate.x == -1) {
            currentCoordinate.x = x;
            currentCoordinate.y = y;
        } else {
            saveCoordinates(x, y);
            connectNodes();
            // create text field for the movie
            var movieInputX = findMidPoint(previousCoordinate.x, currentCoordinate.x);
            var movieInputY = findMidPoint(previousCoordinate.y, currentCoordinate.y);
            createInput("movie", movieInputX, movieInputY);
        }
    }

    /**
     * Creates a text input for the user to input an actor or movie
     */
    function createInput(type, x, y) {
        var theNodeId;
        var textField = document.createElement("input");
        textField.type = "text";
        if (type === "actor") {
            theNodeId = type + actorId
            textField.id = theNodeId;
            textField.addEventListener("keyup", function(e) {
                e.preventDefault();
                if (e.keyCode == 13) {
                    var xhttp = new XMLHttpRequest();
                    xhttp.open("GET", "localhost:8080/v1/users/foo%40bar%2Ecom?api_key=1", false);
                    xhttp.setRequestHeader("Content-type", "application/json");
                    xhttp.send();
                    var response = JSON.parse(xhttp.responseText);
                    textField.style.border = "2px solid green";
                    textField.value = responseText;
                }
            })
            actorId += 1;
        } else if (type === "movie") {
            theNodeId = type + movieId;
            textField.id = theNodeId;
            movieId += 1;
        }
        textField.style.position = "absolute";
        textField.style.left = x + "px";
        textField.style.top = y + "px";
        textField.style.width = NODE_WIDTH - PADDING * 2 + "px";
        textField.style.zIndex = "1";
        document.getElementById("inputLayer").appendChild(textField);
        document.getElementById(theNodeId).focus();
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
        if (previous > current) {
            var length = previous - current;
            return previous - (length >> 1);
        } else {
            var length = current - previous;
            return previous + (length >> 1);
        }
    }

    /**
     * Returns the canvas context
     */
    function context() {
        return document.getElementById(domObject).getContext("2d");
    }
}