function Actor(data) {

    var name, nickname, suffix, id, filmography;

    var dataParser = JSON.parse(data);
    var actorIndex = 0;

    if (dataParser.length > 1) {
        actorIndex = findActorWithMostTitles(dataParser);
    }

    this.name = dataParser[actorIndex].name;
    this.nickname = dataParser[actorIndex].nickname;
    this.suffix = dataParser[actorIndex].suffix;
    this.id = dataParser[actorIndex].id;

    this.filmography = [];

    var i;
    var movies = dataParser[actorIndex].filmography;
    for (i = 0; i < movies.length; i++) {
        this.filmography.push(new MovieWrapper(movies[i].id, movies[i].title, movies[i].year));
    }

    this.print = function(x) {
        var str = this.name + ": " + this.id + "<br/>";
        str += "movies:" + "<br/>";
        var i;
        for (i = 0; i < this.filmography.length; i++) {
            str += "&nbsp;&nbsp;&nbsp;&nbsp;" + this.filmography[i].title + " (" + this.filmography[i].year + ")<br/>";
        }
        document.getElementById(x).innerHTML = str;
    }

    function findActorWithMostTitles(parser) {
        var i = 0;
        var max = 0;
        var indexOfMax = -1;
        for (; i < parser.length; i++) {
            var numberOfTitles = parser[i].filmography.length;
            // if actor at index i has the most films
            if (numberOfTitles > max) {
                // set max and remember the index of the actor
                max = numberOfTitles;
                indexOfMax = i;
            }
        }
        return indexOfMax;
    }

    function MovieWrapper(id, title, year) {
        var id, title, year

        this.id = id;
        this.title = title;
        this.year = year;
    }
}