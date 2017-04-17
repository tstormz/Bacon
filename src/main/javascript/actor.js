function Actor(data) {

    var name, nickname, suffix, id, filmography;

    var dataParser = JSON.parse(data);

    this.name = dataParser[0].name;
    this.nickname = dataParser[0].nickname;
    this.suffix = dataParser[0].suffix;
    this.id = dataParser[0].id;

    this.filmography = [];

    var i;
    var movies = dataParser[0].filmography;
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

    function MovieWrapper(id, title, year) {
        var id, title, year;

        this.id = id;
        this. title = title;
        this.year = year;
    }

}