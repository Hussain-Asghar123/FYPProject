package com.example.fypproject.Network

import com.example.fypproject.DTO.AccountResponse
import com.example.fypproject.DTO.CreateAccountRequest
import com.example.fypproject.DTO.CreateSeasonRequest
import com.example.fypproject.DTO.CreateTeamRequestDto
import com.example.fypproject.DTO.CreateTeamResponseDto
import com.example.fypproject.DTO.FixturesRequest
import com.example.fypproject.DTO.FixturesResponse
import com.example.fypproject.DTO.LoginRequest
import com.example.fypproject.DTO.LoginResponse
import com.example.fypproject.DTO.MatchResponse
import com.example.fypproject.DTO.MediaDto
import com.example.fypproject.DTO.PlayerDto
import com.example.fypproject.DTO.PlayerRequest
import com.example.fypproject.DTO.PlayerRequestDto
import com.example.fypproject.DTO.PlayerResponse
import com.example.fypproject.DTO.PlayerStatsDto
import com.example.fypproject.DTO.PtsTableDto
import com.example.fypproject.DTO.Season
import com.example.fypproject.DTO.SeasonResponse
import com.example.fypproject.DTO.SeasonSportsRequest
import com.example.fypproject.DTO.SportTournamentCount
import com.example.fypproject.DTO.TeamDTO
import com.example.fypproject.DTO.TeamPlayerDto
import com.example.fypproject.DTO.TeamRequest
import com.example.fypproject.DTO.TeamRequestDto
import com.example.fypproject.DTO.TeamResponse
import com.example.fypproject.DTO.TopVotedPlayerDto
import com.example.fypproject.DTO.TournamentOverviewResponse
import com.example.fypproject.DTO.TournamentRequest
import com.example.fypproject.DTO.TournamentResponse
import com.example.fypproject.DTO.TournamentStatsDto
import com.example.fypproject.DTO.TournamentUpdateRequest
import com.example.fypproject.DTO.UpdateAccountRequest
import com.example.fypproject.ScoringDTO.Ball
import com.example.fypproject.ScoringDTO.MatchSummaryDto
import com.example.fypproject.ScoringDTO.ScorecardResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @POST("account/login")
    suspend fun login(
        @Body request: LoginRequest
    ): LoginResponse

    @GET("match/sport")
    suspend fun getLiveMatches(
        @Query(value ="status") status: String,
        @Query(value ="name") sport: String?
    ): Response<List<MatchResponse>>

    @GET("match/sport")
    fun getMatchesBySport(
        @Query("name") name: String?,
        @Query("status") status: String
    ): Call<List<MatchResponse>>

    @POST("account")
    suspend fun createAccount(
        @Body request: CreateAccountRequest
    ): AccountResponse

    @GET("account")
    suspend fun getAllAccounts(): Response<List<AccountResponse>>

    @DELETE("account/{id}")
    suspend fun deleteAccount(
        @Path("id") id: Int
    ): Response<Unit>

    @PUT("account/{id}")
    suspend fun updateAccount(
        @Path("id") id: Long,
        @Body request: UpdateAccountRequest
    )

    @GET("season/names")
    suspend fun getonlynames(): Response<List<SeasonResponse>>

    @POST("season")
    suspend fun createSeason(
        @Body request: CreateSeasonRequest
    ): SeasonResponse

    @GET("season/{id}")
    suspend fun getSeasonById(
        @Path("id") id: Long
    ): Response<List<SportTournamentCount>>

    @POST("add-sports")
    fun addSportsToSeason(@Body request: SeasonSportsRequest): Call<Season>

    @GET("season/tournaments/{id}/{sid}")
    fun getSeasonWiseTournaments(
        @Path("id") seasonId: Long,
        @Path("sid") sportId: Long
    ): Call<List<TournamentResponse>>

    @POST("tournament")
    fun createTournament(
        @Body tournament: TournamentRequest
    ): Call<Void>

    @GET("tournament/{id}")
    fun getTournamentById(
        @Path("id") id: Long
    ): Call<TournamentUpdateRequest>

    @PUT("tournament/{id}")
    fun updateTournament(
        @Path("id") id: Long,
        @Body dto: TournamentUpdateRequest
    ): Call<TournamentUpdateRequest>

    @GET("tournament/overview/{id}")
    suspend fun getTournamentOverview(
        @Path("id") id: Long
    ): Response<TournamentOverviewResponse>

    @GET("match/scorer/{id}")
    suspend fun getMatchesByScorer(@Path("id") scorerId: Long): Response<List<MatchResponse>>

    //backend sends name,id in map in response body
    @GET("team/tournament/{id}")
    suspend fun getTeamsByTournamentId(@Path("id") id: Long):
            Response<List<TeamDTO>>

    @GET("match/tournament/{tournamentId}")
    suspend fun getMatchesByTournament(
        @Path("tournamentId") tournamentId: Long
    ): Response<List<FixturesResponse>>

    @POST("match")
    suspend fun createFixture(
        @Body request: FixturesRequest
    ): Response<Unit>

    @GET("match/{id}")
    suspend fun getMatchById(@Path("id") id: Long): Response<FixturesResponse>

    @PUT("match/{id}")
    suspend fun updateMatch(
        @Path("id") id: Long,
        @Body matchDTO: FixturesResponse
    ): Response<Unit>

    @PUT("player/{id}")
    suspend fun updatePlayer(
        @Path("id") id: Long,
        @Body player: PlayerDto
    ): Response<PlayerDto>

    @GET("team/tournament/{tid}")
    suspend fun getTeamsByTournament(@Path("tid") tid: Long): Response<List<TeamDTO>>

    @GET("team/tournament/account/{tid}/{aid}")
    suspend fun getTeamByTournamentAndAccount(
        @Path("tid") tournamentId: Long,
        @Path("aid") accountId: Long
    ): Response<TeamResponse>

    @POST("team/{id}/{playerId}")
    suspend fun createTeam(
        @Path("id") tournamentId: Long,
        @Path("playerId") playerId: Long,
        @Body request: CreateTeamRequestDto
    ): Response<CreateTeamResponseDto>

    @POST("playerRequest")
    suspend fun createPlayerRequest(
        @Body request: PlayerRequest
    ): Response<ResponseBody>

    @POST("teamRequest")
    suspend fun createTeamRequest(
        @Body request: TeamRequest
    ): Response<ResponseBody>

    @GET("account/players/{tid}")
    suspend fun getAllPlayerAccounts(
        @Path("tid") teamId: Long
    ): Response<List<PlayerResponse>>

    @GET("/ptsTable/tournament/{tournamentId}")
    suspend fun getPtsTablesByTournament(@Path("tournamentId") tournamentId: Long): Response<List<PtsTableDto>>



    @GET("playerRequest/player/{playerId}")
    fun getPlayerRequests(@Path("playerId") playerId: Long): Call<List<PlayerRequestDto>>

    @PUT("playerRequest/approve/{id}")
    fun approvePlayerRequest(@Path("id") id: Long): Call<Void>

    @PUT("playerRequest/reject/{id}")
    fun rejectPlayerRequest(@Path("id") id: Long): Call<Void>

    @GET("teamRequest")
    fun getAllTeamRequests(): Call<List<TeamRequestDto>>

    @PUT("teamRequest/approve/{id}")
    fun approveTeamRequest(@Path("id") id: Long): Call<Void>

    @PUT("teamRequest/reject/{id}")
    fun rejectTeamRequest(@Path("id") id: Long): Call<Void>

    @GET("media/season/{id}/{page}/{size}")
    fun getMediaBySeasonId(
        @Path("id") id: Long,
        @Path("page") page: Int,
        @Path("size") size: Int
    ): Call<List<MediaDto>>

    @GET("media/tournament/{id}/{page}/{size}")
    fun getMediaByTournamentId(
        @Path("id") id: Long,
        @Path("page") page: Int,
        @Path("size") size: Int
    ): Call<List<MediaDto>>

    @GET("media/sport/{id}/{page}/{size}")
    fun getMediaBySportId(
        @Path("id") id: Long,
        @Path("page") page: Int,
        @Path("size") size: Int
    ): Call<List<MediaDto>>

    @GET("player/{playerId}/stats")
    suspend fun getPlayerStats(
        @Path("playerId") playerId: Long,
        @Query("tournamentId") tournamentId: Long? = null,
        @Query("sport") sport: String? = null
    ): PlayerStatsDto

    @GET("player/{playerId}/stats")
    suspend fun getPlayerTournamentStats(
        @Path("playerId") playerId: Long,
        @Query("tournamentId") tournamentId: Long
    ): PlayerStatsDto

    @GET("tournament/{tournamentId}/stats")
    suspend fun getTournamentStats(
        @Path("tournamentId") tournamentId: Long
    ): TournamentStatsDto

    @GET("tournament/namesAndIds")
    suspend fun getTournamentNamesAndIds(): List<Map<Long, String>>

    @GET("/match/{id}")
    suspend fun getMatchById1(@Path("id") id: Long): Response<MatchResponse>

    @PUT("/match/start/{id}")
    suspend fun startMatch(
        @Path("id") id: Long,
        @Body match: MatchResponse
    ): Response<ResponseBody>

    @PUT("/match/abandon/{id}")
    suspend fun abandonMatch(@Path("id") id: Long): Response<Any>

    @GET("team/{teamId}/players")
    suspend fun getPlayersByTeam(@Path("teamId") teamId: Long): Response<List<TeamPlayerDto>>

    @GET("match/scoreCard/{Mid}/{T1id}")
    suspend fun getScoreCard(
        @Path("Mid") matchId: Long,
        @Path("T1id") teamId: Long
    ): Response<ScorecardResponse>

    @GET("match/balls/{mid}/{tid}")
    suspend fun getMatchBalls(
        @Path("mid") matchId: Long,
        @Path("tid") teamId: Long
    ): Response<List<Ball>>

    @GET("/match/summary/{mid}")
    suspend fun getMatchSummary
                (@Path("mid") mid: Long
    ): Response<MatchSummaryDto>

    @Multipart
    @POST("media")
    suspend fun createMedia(
        @Part("matchId") matchId: RequestBody,
        @Part("ballId") ballId: RequestBody,
        @Part file: MultipartBody.Part
    ): Response<Unit>

    @POST("api/favourite-player/vote")
    suspend fun submitVote(
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): Response<Unit>

    @GET("api/favourite-player/top-voted/{tournamentId}")
    suspend fun getTopVotedPlayers(@Path("tournamentId") tournamentId: Long): List<TopVotedPlayerDto>

    @POST("api/favourite-player/set-mot/{tournamentId}/{playerId}")
    suspend fun setManOfTournament(
        @Path("tournamentId") tournamentId: Long,
        @Path("playerId") playerId: Long
    ): ResponseBody

}