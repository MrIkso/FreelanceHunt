package tech.hombre.data.repository.feedlist

import tech.hombre.data.database.dao.FeedListDao
import tech.hombre.data.database.model.FeedListEntity
import tech.hombre.data.networking.FeedApi
import tech.hombre.data.networking.base.getData
import tech.hombre.data.repository.BaseRepository
import tech.hombre.domain.model.FeedList
import tech.hombre.domain.model.Result
import tech.hombre.domain.repository.FeedListRepository

class FeedListRepositoryImpl(
    private val feedApi: FeedApi,
    private val feedListDao: FeedListDao
) : BaseRepository<FeedList, FeedListEntity>(),
    FeedListRepository {
    override suspend fun getFeedList(): Result<FeedList> {
        return fetchData(
            apiDataProvider = {
                feedApi.getFeedList().getData(
                    fetchFromCacheAction = { feedListDao.getFeedList("my/feed") },
                    cacheAction = { feedListDao.saveFeedList(it) })
            },
            dbDataProvider = { feedListDao.getFeedList("my/feed") }
        )
    }
}