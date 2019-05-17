package com.unsplash.pickerandroid.photopicker.domain

import androidx.lifecycle.MutableLiveData
import androidx.paging.PageKeyedDataSource
import com.unsplash.pickerandroid.photopicker.UnsplashPhotoPicker
import com.unsplash.pickerandroid.photopicker.data.NetworkEndpoints
import com.unsplash.pickerandroid.photopicker.data.UnsplashPhoto
import com.unsplash.pickerandroid.photopicker.data.SearchResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Android paging library data source.
 * This will load the photos for the search and allow an infinite scroll on the picker screen.
 */
class SearchPhotoDataSource(
    private val networkEndpoints: NetworkEndpoints,
    private val criteria: String
) : PageKeyedDataSource<Int, UnsplashPhoto>() {

    val networkState = MutableLiveData<NetworkState>()

    private var lastPage: Int? = null

    override fun loadInitial(params: LoadInitialParams<Int>, callback: LoadInitialCallback<Int, UnsplashPhoto>) {
        // updating the network state to loading
        networkState.postValue(NetworkState.LOADING)
        // api call for the first page
        networkEndpoints.searchPhotos(
            UnsplashPhotoPicker.getAccessKey(),
            criteria,
            1,
            params.requestedLoadSize
        )
            .enqueue(object : Callback<SearchResponse> {
                override fun onFailure(call: Call<SearchResponse>, t: Throwable) {
                    // we update the network state to error along with the error message
                    networkState.postValue(NetworkState.error(t.message))
                }

                override fun onResponse(call: Call<SearchResponse>, response: Response<SearchResponse>) {
                    // if the response is successful
                    // we get the last page number
                    // we push the result on the paging callback
                    // we update the network state to success
                    if (response.isSuccessful) {
                        lastPage = response.headers().get("x-total")?.toInt()?.div(params.requestedLoadSize)
                        val list = response.body()?.results!!
                        callback.onResult(list, null, 2)
                        if (list.isEmpty()) {
                            networkState.postValue(NetworkState.EMPTY)
                        } else {
                            networkState.postValue(NetworkState.SUCCESS)
                        }
                    }
                    // if the response is not successful
                    // we update the network state to error along with the error message
                    else {
                        networkState.postValue(NetworkState.error(response.message()))
                    }
                }
            })
    }

    override fun loadAfter(params: LoadParams<Int>, callback: LoadCallback<Int, UnsplashPhoto>) {
        // updating the network state to loading
        networkState.postValue(NetworkState.LOADING)
        // api call for the subsequent pages
        networkEndpoints.searchPhotos(
            UnsplashPhotoPicker.getAccessKey(),
            criteria,
            params.key,
            params.requestedLoadSize
        )
            .enqueue(object : Callback<SearchResponse> {
                override fun onFailure(call: Call<SearchResponse>, t: Throwable) {
                    // we update the network state to error along with the error message
                    networkState.postValue(NetworkState.error(t.message))
                }

                override fun onResponse(call: Call<SearchResponse>, response: Response<SearchResponse>) {
                    // if the response is successful
                    // we get the next page number
                    // we push the result on the paging callback
                    // we update the network state to success
                    if (response.isSuccessful) {
                        val nextPage = if (params.key == lastPage) null else params.key + 1
                        callback.onResult(response.body()?.results!!, nextPage)
                        networkState.postValue(NetworkState.SUCCESS)
                    }
                    // if the response is not successful
                    // we update the network state to error along with the error message
                    else {
                        networkState.postValue(NetworkState.error(response.message()))
                    }
                }
            })
    }

    override fun loadBefore(params: LoadParams<Int>, callback: LoadCallback<Int, UnsplashPhoto>) {
        // we do nothing here because everything will be loaded
    }
}
