package tech.hombre.data.networking.model

import com.google.gson.annotations.SerializedName
import tech.hombre.domain.model.MyBidsList

data class ProposeConditionsBody(
    val days: Int,
    val budget: MyBidsList.Data.Attributes.Budget,
    @SerializedName("safe_type")
    val safeType: String,
    val comment: String
)