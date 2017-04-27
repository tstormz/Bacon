function Movie(movieData) {

    this.id = movieData.id;
    this.title = movieData.title;
    this.year = movieData.year;
    this.cast = [];

    var i = 0;
    for (; i < movieData.cast.length; i++) {
        this.cast.push(movieData.cast[i].id);
    }

    this.print = function(x) {
        var str = this.title + " (" + this.year + ")<br/>cast: ";
        var i = 0;
        for (; i < this.cast.length; i++) {
            str += "&nbsp;&nbsp;&nbsp;&nbsp;" + this.cast[i] + "<br/>";
        }
        document.getElementById(x).innerHTML = str;
    };

    this.verifyActors = function(a1, a2) {
        var foundA1, foundA2;
        foundA1 = false;
        foundA2 = false;
        var i = 0;
        for (; i < this.cast.length; i++) {
            if (a1 === this.cast[i]) {
                foundA1 = true;
            } else if (a2 === this.cast[i]) {
                foundA2 = true;
            }
            if (foundA1 && foundA2) {
                return true;
            }
        }
        return false;
    };

}