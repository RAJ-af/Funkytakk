package com.example.ui.screens.home.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.screens.home.models.MomentPost

@Composable
fun MomentsTabContent(
    momentsPosts: List<MomentPost>,
    onToggleLike: (MomentPost) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3F4F6))
    ) {
        item {
            TabRow(
                selectedTabIndex = 0,
                containerColor = Color.White
            ) {
                Tab(selected = true, onClick = {}, text = { Text("Trending", fontWeight = FontWeight.Bold) })
                Tab(selected = false, onClick = {}, text = { Text("My Language") })
            }
        }

        items(momentsPosts) { post ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp, horizontal = 12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = post.avatar,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(post.author, fontWeight = FontWeight.SemiBold, color = Color.Black, fontSize = 15.sp)
                            Text("@" + post.username + " • " + post.timeAgo, color = Color.Gray, fontSize = 12.sp)
                        }
                        Icon(Icons.Default.MoreVert, contentDescription = null, tint = Color.Gray)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(post.content, color = Color.Black, fontSize = 14.sp, lineHeight = 20.sp)

                    if (post.imageUrl != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        AsyncImage(
                            model = post.imageUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Like interaction
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onToggleLike(post) }
                                .padding(6.dp)
                        ) {
                            Icon(
                                imageVector = if (post.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Likes",
                                tint = if (post.isLiked) Color.Red else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${if (post.isLiked) post.initialLikes + 1 else post.initialLikes}",
                                color = Color.DarkGray,
                                fontSize = 13.sp
                            )
                        }

                        // Comments counter display
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(6.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Outlined.Chat,
                                contentDescription = "Comments",
                                tint = Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("${post.commentsCount}", color = Color.DarkGray, fontSize = 13.sp)
                        }

                        Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.Gray, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}
