package pse.at.swivl.ui.main

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.IOException
import pse.at.swivl.ui.base.AppRepository
import pse.at.swivl.ui.main.domain.models.Movie
import pse.at.swivl.ui.main.domain.models.MoviePicture
import pse.at.swivl.ui.main.domain.models.Photos
import pse.at.swivl.ui.utils.Utils

/**
 * The main repository providing online/offline access to movie and its specific data.
 */
object MainRepository : AppRepository() {
    private val mGson = Gson()
    private val listType = object : TypeToken<List<Movie>?>() {}.type
    private val movieDao = getOfflineDatabase().getMovieDao()

    suspend fun findMoviesByTitle(title: String, maxResults: Int, rating: Int) =
        movieDao.findMoviesByTitle("%$title%", maxResults, rating)


    suspend fun loadMovies(callback: (List<Movie>) -> Unit) {
        var moviesList = movieDao.getMovies()
        if (moviesList.isEmpty()) {
            moviesList = withContext(Dispatchers.IO) {
                mGson.fromJson(Utils.loadJSONStringFromAsset(context)!!, listType)
            }
            movieDao.addMovies(moviesList)

            moviesList = movieDao.getMovies()
        }

        callback(moviesList)

    }

    suspend fun findMovieByTitle(title: String) = movieDao.findMovieByTitle(title)


    suspend fun searchMoviePictures(title: String): List<MoviePicture>? =
        withContext(Dispatchers.IO) {
            var pictures: List<MoviePicture>? = null
            try {
                val body = apiClient.searchPhotos(title).string()
                val json = body.substringAfter("(").substringBefore("})") + "}"
                pictures = mGson.fromJson(json, Photos::class.java).photos.pictures
            } catch (e: IOException) {
                e.printStackTrace()
            }
            pictures
        }

}