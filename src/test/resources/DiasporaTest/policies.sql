-- policies must be one per line, comments must start at start of line
-- * policies not supported
-- my_info
SELECT users.id, users.username, users.serialized_private_key, users.getting_started, users.disable_mail, users."language", users.email, users.encrypted_password, users.reset_password_token, users.remember_created_at, users.sign_in_count, users.current_sign_in_at, users.last_sign_in_at, users.current_sign_in_ip, users.last_sign_in_ip, users.created_at, users.updated_at, users.invited_by_id, users.authentication_token, users.unconfirmed_email, users.confirm_email_token, users.locked_at, users.show_community_spotlight_in_stream, users.auto_follow_back, users.auto_follow_back_aspect_id, users.hidden_shareables, users.reset_password_sent_at, users.last_seen, users.remove_after, users.export, users.exported_at, users.exporting, users.strip_exif, users.exported_photos_file, users.exported_photos_at, users.exporting_photos, users.color_theme, users.post_default_public, users.consumed_timestep, users.otp_required_for_login, users.otp_backup_codes, users.plain_otp_secret FROM users WHERE users.id = _MY_UID;
SELECT people.id, people.guid, people.diaspora_handle, people.serialized_public_key, people.owner_id, people.created_at, people.updated_at, people.closed_account, people.fetch_status, people.pod_id FROM people WHERE people.owner_id = _MY_UID;
SELECT profiles.id, profiles.diaspora_handle, profiles.first_name, profiles.last_name, profiles.image_url, profiles.image_url_small, profiles.image_url_medium, profiles.birthday, profiles.gender, profiles.bio, profiles.searchable, profiles.person_id, profiles.created_at, profiles.updated_at, profiles.location, profiles.full_name, profiles.nsfw, profiles.public_details FROM profiles, people WHERE profiles.person_id = people.id AND people.owner_id = _MY_UID;
-- visible_posts
SELECT posts.id, posts.author_id, posts."public", posts.guid, posts.type, posts.text, posts.created_at, posts.updated_at, posts.provider_display_name, posts.root_guid, posts.likes_count, posts.comments_count, posts.o_embed_cache_id, posts.reshares_count, posts.interacted_at, posts.tweet_id, posts.open_graph_cache_id, posts.tumblr_ids FROM posts WHERE posts."public" <> FALSE;
SELECT posts.id, posts.author_id, posts."public", posts.guid, posts.type, posts.text, posts.created_at, posts.updated_at, posts.provider_display_name, posts.root_guid, posts.likes_count, posts.comments_count, posts.o_embed_cache_id, posts.reshares_count, posts.interacted_at, posts.tweet_id, posts.open_graph_cache_id, posts.tumblr_ids FROM posts, people WHERE posts.author_id = people.id AND people.owner_id = _MY_UID;
SELECT posts.id, posts.author_id, posts."public", posts.guid, posts.type, posts.text, posts.created_at, posts.updated_at, posts.provider_display_name, posts.root_guid, posts.likes_count, posts.comments_count, posts.o_embed_cache_id, posts.reshares_count, posts.interacted_at, posts.tweet_id, posts.open_graph_cache_id, posts.tumblr_ids FROM posts, share_visibilities WHERE share_visibilities.shareable_id = posts.id AND share_visibilities.shareable_type = 'Post' AND share_visibilities.user_id = _MY_UID;
-- visible_comments
SELECT comments.id, comments.text, comments.commentable_id, comments.author_id, comments.guid, comments.created_at, comments.updated_at, comments.likes_count, comments.commentable_type FROM comments, posts WHERE comments.commentable_type = 'Post' AND comments.commentable_id = posts.id AND posts."public" <> FALSE;
SELECT comments.id, comments.text, comments.commentable_id, comments.author_id, comments.guid, comments.created_at, comments.updated_at, comments.likes_count, comments.commentable_type FROM comments, posts, people WHERE comments.commentable_type = 'Post' AND comments.commentable_id = posts.id AND posts.author_id = people.id AND people.owner_id = _MY_UID;
SELECT comments.id, comments.text, comments.commentable_id, comments.author_id, comments.guid, comments.created_at, comments.updated_at, comments.likes_count, comments.commentable_type FROM comments, posts, share_visibilities  WHERE comments.commentable_type = 'Post' AND comments.commentable_id = posts.id AND share_visibilities.shareable_id = posts.id AND share_visibilities.shareable_type = 'Post' AND share_visibilities.user_id = _MY_UID;
-- visible_mentions
SELECT mentions.id, mentions.mentions_container_id, mentions.person_id, mentions.mentions_container_type FROM mentions, posts WHERE mentions.mentions_container_type = 'Post' AND mentions.mentions_container_id = posts.id AND posts."public" <> FALSE;
SELECT mentions.id, mentions.mentions_container_id, mentions.person_id, mentions.mentions_container_type FROM mentions, posts, people WHERE mentions.mentions_container_type = 'Post' AND mentions.mentions_container_id = posts.id AND posts.author_id = people.id AND people.owner_id = _MY_UID;
SELECT mentions.id, mentions.mentions_container_id, mentions.person_id, mentions.mentions_container_type FROM mentions, posts, share_visibilities WHERE mentions.mentions_container_type = 'Post' AND mentions.mentions_container_id = posts.id AND share_visibilities.shareable_id = posts.id AND share_visibilities.shareable_type = 'Post' AND share_visibilities.user_id = _MY_UID;
SELECT mentions.id, mentions.mentions_container_id, mentions.person_id, mentions.mentions_container_type FROM mentions, comments, posts WHERE mentions.mentions_container_type = 'Comment' AND mentions.mentions_container_id = comments.id AND comments.commentable_type = 'Post' AND comments.commentable_id = posts.id AND posts."public" <> FALSE;
SELECT mentions.id, mentions.mentions_container_id, mentions.person_id, mentions.mentions_container_type FROM mentions, comments, posts, people WHERE mentions.mentions_container_type = 'Comment' AND mentions.mentions_container_id = comments.id AND comments.commentable_type = 'Post' AND comments.commentable_id = posts.id AND posts.author_id = people.id AND people.owner_id = _MY_UID;
SELECT mentions.id, mentions.mentions_container_id, mentions.person_id, mentions.mentions_container_type FROM mentions, comments, posts, share_visibilities  WHERE mentions.mentions_container_type = 'Comment' AND mentions.mentions_container_id = comments.id AND comments.commentable_type = 'Post' AND comments.commentable_id = posts.id AND share_visibilities.shareable_id = posts.id AND share_visibilities.shareable_type = 'Post' AND share_visibilities.user_id = _MY_UID;

-- all_people
-- isn't this a strict superset of my_info[1]?
SELECT people.id, people.guid, people.diaspora_handle, people.serialized_public_key, people.owner_id, people.created_at, people.updated_at, people.closed_account, people.fetch_status, people.pod_id FROM people;

-- basic_profiles
SELECT profiles.id, profiles.diaspora_handle, profiles.first_name, profiles.last_name, profiles.image_url, profiles.image_url_small, profiles.image_url_medium, profiles.created_at, profiles.updated_at, profiles.full_name, profiles.nsfw, profiles.public_details, profiles.searchable, profiles.person_id FROM profiles;

-- extended_profiles
SELECT profiles.id, profiles.bio, profiles.location, profiles.gender, profiles.birthday FROM profiles;

-- visible_photos
SELECT photos.id, photos.author_id, photos.public, photos.guid, photos.pending, photos.text, photos.remote_photo_path, photos.remote_photo_name, photos.random_string, photos.processed_image, photos.created_at, photos.updated_at, photos.unprocessed_image, photos.status_message_guid, photos.height, photos.width FROM photos, people WHERE photos.author_id = people.id AND photos.pending = FALSE AND people.owner_id = _MY_UID;
SELECT photos.id, photos.author_id, photos.public, photos.guid, photos.pending, photos.text, photos.remote_photo_path, photos.remote_photo_name, photos.random_string, photos.processed_image, photos.created_at, photos.updated_at, photos.unprocessed_image, photos.status_message_guid, photos.height, photos.width FROM photos WHERE photos.public <> FALSE AND photos.pending = FALSE;
SELECT photos.id, photos.author_id, photos.public, photos.guid, photos.pending, photos.text, photos.remote_photo_path, photos.remote_photo_name, photos.random_string, photos.processed_image, photos.created_at, photos.updated_at, photos.unprocessed_image, photos.status_message_guid, photos.height, photos.width FROM photos, share_visibilities WHERE share_visibilities.shareable_id = photos.id AND share_visibilities.shareable_type = 'Photo' AND share_visibilities.user_id = _MY_UID AND photos.pending = FALSE;
SELECT photos.id, photos.author_id, photos.public, photos.guid, photos.pending, photos.text, photos.remote_photo_path, photos.remote_photo_name, photos.random_string, photos.processed_image, photos.created_at, photos.updated_at, photos.unprocessed_image, photos.status_message_guid, photos.height, photos.width FROM photos, posts WHERE photos.status_message_guid = posts.guid AND posts."public" <> FALSE;
SELECT photos.id, photos.author_id, photos.public, photos.guid, photos.pending, photos.text, photos.remote_photo_path, photos.remote_photo_name, photos.random_string, photos.processed_image, photos.created_at, photos.updated_at, photos.unprocessed_image, photos.status_message_guid, photos.height, photos.width FROM photos, posts, people WHERE photos.status_message_guid = posts.guid AND posts.author_id = people.id AND people.owner_id = _MY_UID;
SELECT photos.id, photos.author_id, photos.public, photos.guid, photos.pending, photos.text, photos.remote_photo_path, photos.remote_photo_name, photos.random_string, photos.processed_image, photos.created_at, photos.updated_at, photos.unprocessed_image, photos.status_message_guid, photos.height, photos.width FROM photos, posts, share_visibilities WHERE photos.status_message_guid = posts.guid AND share_visibilities.shareable_id = posts.id AND share_visibilities.shareable_type = 'Post' AND share_visibilities.user_id = _MY_UID;

-- visible_locations
SELECT locations.id, locations.address, locations.lat, locations.lng, locations.status_message_id, locations.created_at, locations.updated_at FROM locations, posts WHERE locations.status_message_id = posts.id AND posts."public" <> FALSE;
SELECT locations.id, locations.address, locations.lat, locations.lng, locations.status_message_id, locations.created_at, locations.updated_at FROM locations, posts, people WHERE locations.status_message_id = posts.id AND posts.author_id = people.id AND people.owner_id = _MY_UID;
SELECT locations.id, locations.address, locations.lat, locations.lng, locations.status_message_id, locations.created_at, locations.updated_at FROM locations, posts, share_visibilities WHERE locations.status_message_id = posts.id AND share_visibilities.shareable_id = posts.id AND share_visibilities.shareable_type = 'Post' AND share_visibilities.user_id = _MY_UID;

-- visible_polls
SELECT polls.id, polls.question, polls.status_message_id, polls.status, polls.guid, polls.created_at, polls.updated_at FROM polls, posts WHERE polls.status_message_id = posts.id AND posts."public" <> FALSE;
SELECT polls.id, polls.question, polls.status_message_id, polls.status, polls.guid, polls.created_at, polls.updated_at FROM polls, posts, people WHERE polls.status_message_id = posts.id AND posts.author_id = people.id AND people.owner_id = _MY_UID;
SELECT polls.id, polls.question, polls.status_message_id, polls.status, polls.guid, polls.created_at, polls.updated_at FROM polls, posts, share_visibilities WHERE polls.status_message_id = posts.id AND share_visibilities.shareable_id = posts.id AND share_visibilities.shareable_type = 'Post' AND share_visibilities.user_id = _MY_UID;

-- visible_participations
SELECT participations.id, participations.guid, participations.target_id, participations.target_type, participations.author_id, participations.created_at, participations.updated_at, participations."count" FROM participations, people WHERE participations.author_id = people.id AND people.owner_id = _MY_UID;

-- visible_likes
SELECT likes.id, likes.positive, likes.target_id, likes.author_id, likes.guid, likes.created_at, likes.updated_at, likes.target_type FROM likes, posts WHERE likes.target_id = posts.id AND likes.target_type = 'Post' AND posts."public" <> FALSE;
SELECT likes.id, likes.positive, likes.target_id, likes.author_id, likes.guid, likes.created_at, likes.updated_at, likes.target_type FROM likes, posts, people WHERE likes.target_id = posts.id AND likes.target_type = 'Post' AND posts.author_id = people.id AND people.owner_id = _MY_UID;
SELECT likes.id, likes.positive, likes.target_id, likes.author_id, likes.guid, likes.created_at, likes.updated_at, likes.target_type FROM likes, posts, share_visibilities WHERE likes.target_id = posts.id AND likes.target_type = 'Post' AND share_visibilities.shareable_id = posts.id AND share_visibilities.shareable_type = 'Post' AND share_visibilities.user_id = _MY_UID;

-- visible_tags
SELECT tags.id, tags.name, tags.taggings_count, taggings.id, taggings.tag_id, taggings.taggable_id, taggings.taggable_type, taggings.tagger_id, taggings.tagger_type, taggings.context, taggings.created_at FROM tags, taggings, posts WHERE tags.id = taggings.tag_id AND taggings.taggable_id = posts.id AND taggings.taggable_type = 'Post' AND posts."public" <> FALSE;
SELECT tags.id, tags.name, tags.taggings_count, taggings.id, taggings.tag_id, taggings.taggable_id, taggings.taggable_type, taggings.tagger_id, taggings.tagger_type, taggings.context, taggings.created_at FROM tags, taggings, posts, people WHERE tags.id = taggings.tag_id AND taggings.taggable_id = posts.id AND taggings.taggable_type = 'Post' AND posts.author_id = people.id AND people.owner_id = _MY_UID;
SELECT tags.id, tags.name, tags.taggings_count, taggings.id, taggings.tag_id, taggings.taggable_id, taggings.taggable_type, taggings.tagger_id, taggings.tagger_type, taggings.context, taggings.created_at FROM tags, taggings, posts, share_visibilities WHERE tags.id = taggings.tag_id AND taggings.taggable_id = posts.id AND taggings.taggable_type = 'Post' AND share_visibilities.shareable_id = posts.id AND share_visibilities.shareable_type = 'Post' AND share_visibilities.user_id = _MY_UID;

-- visible_notificatoins
SELECT notifications.id, notifications.target_type, notifications.target_id, notifications.recipient_id, notifications.unread, notifications.created_at, notifications.updated_at, notifications.type, notifications.guid FROM notifications WHERE notifications.recipient_id = _MY_UID;

-- visible_conv
SELECT conversations.id, conversations.subject, conversations.guid, conversations.author_id, conversations.created_at, conversations.updated_at FROM conversations, people WHERE conversations.author_id = people.id AND people.owner_id = _MY_UID;
SELECT conversation_visibilities.id, conversation_visibilities.conversation_id, conversation_visibilities.person_id, conversation_visibilities.unread, conversation_visibilities.created_at, conversation_visibilities.updated_at FROM conversation_visibilities, people WHERE conversation_visibilities.person_id = people.id AND people.owner_id = _MY_UID;

-- visible_roles
SELECT roles.id, roles.person_id, roles.name, roles.created_at, roles.updated_at FROM roles, people WHERE roles.person_id = people.id AND people.id = _MY_UID;

-- visible_mine
SELECT aspects.id, aspects.name, aspects.user_id, aspects.created_at, aspects.updated_at, aspects.order_id, aspects.post_default FROM aspects WHERE aspects.user_id = _MY_UID;
SELECT services.id, services.type, services.user_id, services.uid, services.access_token, services.access_secret, services.nickname, services.created_at, services.updated_at FROM services WHERE services.user_id = _MY_UID;
SELECT contacts.id, contacts.user_id, contacts.person_id, contacts.created_at, contacts.updated_at, contacts.sharing, contacts.receiving FROM contacts WHERE contacts.user_id = _MY_UID;
SELECT user_preferences.id, user_preferences.email_type, user_preferences.user_id, user_preferences.created_at, user_preferences.updated_at FROM user_preferences WHERE user_preferences.user_id = _MY_UID;
SELECT tag_followings.id,  tag_followings.tag_id,  tag_followings.user_id,  tag_followings.created_at,  tag_followings.updated_at FROM tag_followings WHERE tag_followings.user_id = _MY_UID;

-- visible_by_admin
SELECT users.id, users.username, users.serialized_private_key, users.getting_started, users.disable_mail, users."language", users.email, users.encrypted_password, users.reset_password_token, users.remember_created_at, users.sign_in_count, users.current_sign_in_at, users.last_sign_in_at, users.current_sign_in_ip, users.last_sign_in_ip, users.created_at, users.updated_at, users.invited_by_id, users.authentication_token, users.unconfirmed_email, users.confirm_email_token, users.locked_at, users.show_community_spotlight_in_stream, users.auto_follow_back, users.auto_follow_back_aspect_id, users.hidden_shareables, users.reset_password_sent_at, users.last_seen, users.remove_after, users.export, users.exported_at, users.exporting, users.strip_exif, users.exported_photos_file, users.exported_photos_at, users.exporting_photos, users.color_theme, users.post_default_public, users.consumed_timestep, users.otp_required_for_login, users.otp_backup_codes, users.plain_otp_secret FROM users, roles, people WHERE roles.person_id = people.id AND roles.name = 'admin';
-- todo: self-join
-- SELECT people.id, people.guid, people.diaspora_handle, people.serialized_public_key, people.owner_id, people.created_at, people.updated_at, people.closed_account, people.fetch_status, people.pod_id FROM people, roles, people WHERE roles.person_id = people.id AND roles.name = 'admin';
SELECT posts.id, posts.author_id, posts."public", posts.guid, posts.type, posts.text, posts.created_at, posts.updated_at, posts.provider_display_name, posts.root_guid, posts.likes_count, posts.comments_count, posts.o_embed_cache_id, posts.reshares_count, posts.interacted_at, posts.tweet_id, posts.open_graph_cache_id, posts.tumblr_ids FROM posts, roles, people WHERE roles.person_id = people.id AND roles.name = 'admin';
SELECT share_visibilities.id, share_visibilities.shareable_id, share_visibilities.hidden, share_visibilities.shareable_type, share_visibilities.user_id FROM share_visibilities, roles, people WHERE roles.person_id = people.id AND roles.name = 'admin';
SELECT mentions.id, mentions.mentions_container_id, mentions.person_id, mentions.mentions_container_type FROM mentions, roles, people WHERE roles.person_id = people.id AND roles.name = 'admin';
SELECT comments.id, comments.text, comments.commentable_id, comments.author_id, comments.guid, comments.created_at, comments.updated_at, comments.likes_count, comments.commentable_type FROM comments, roles, people WHERE roles.person_id = people.id AND roles.name = 'admin';
SELECT profiles.id, profiles.diaspora_handle, profiles.first_name, profiles.last_name, profiles.image_url, profiles.image_url_small, profiles.image_url_medium, profiles.birthday, profiles.gender, profiles.bio, profiles.searchable, profiles.person_id, profiles.created_at, profiles.updated_at, profiles.location, profiles.full_name, profiles.nsfw, profiles.public_details FROM profiles, roles, people WHERE roles.person_id = people.id AND roles.name = 'admin';
SELECT photos.id, photos.author_id, photos."public", photos.guid, photos.pending, photos.text, photos.remote_photo_path, photos.remote_photo_name, photos.random_string, photos.processed_image, photos.created_at, photos.updated_at, photos.unprocessed_image, photos.status_message_guid, photos.height, photos.width FROM photos, roles, people WHERE roles.person_id = people.id AND roles.name = 'admin';
SELECT locations.id, locations.address, locations.lat, locations.lng, locations.status_message_id, locations.created_at, locations.updated_at FROM locations, roles, people WHERE roles.person_id = people.id AND roles.name = 'admin';
SELECT polls.id, polls.question, polls.status_message_id, polls.status, polls.guid, polls.created_at, polls.updated_at FROM polls, roles, people WHERE roles.person_id = people.id AND roles.name = 'admin';
SELECT participations.id, participations.guid, participations.target_id, participations.target_type, participations.author_id, participations.created_at, participations.updated_at, participations."count" FROM participations, roles, people WHERE roles.person_id = people.id AND roles.name = 'admin';
SELECT likes.id, likes.positive, likes.target_id, likes.author_id, likes.guid, likes.created_at, likes.updated_at, likes.target_type FROM likes, roles, people WHERE roles.person_id = people.id AND roles.name = 'admin';
SELECT tags.id, tags.name, tags.taggings_count FROM tags, roles, people WHERE roles.person_id = people.id AND roles.name = 'admin';
SELECT taggings.id, taggings.tag_id, taggings.taggable_id, taggings.taggable_type, taggings.tagger_id, taggings.tagger_type, taggings.context, taggings.created_at FROM taggings, roles, people WHERE roles.person_id = people.id AND roles.name = 'admin';
SELECT notifications.id, notifications.target_type, notifications.target_id, notifications.recipient_id, notifications.unread, notifications.created_at, notifications.updated_at, notifications.type, notifications.guid FROM notifications, roles, people WHERE roles.person_id = people.id AND roles.name = 'admin';
SELECT conversations.id, conversations.subject, conversations.guid, conversations.author_id, conversations.created_at, conversations.updated_at FROM conversations, roles, people WHERE roles.person_id = people.id AND roles.name = 'admin';
SELECT conversation_visibilities.id, conversation_visibilities.conversation_id, conversation_visibilities.person_id, conversation_visibilities.unread, conversation_visibilities.created_at, conversation_visibilities.updated_at FROM conversation_visibilities, roles, people WHERE roles.person_id = people.id AND roles.name = 'admin';
-- todo: self-join
-- SELECT roles.id, roles.person_id, roles.name, roles.created_at, roles.updated_at FROM roles, roles, people WHERE roles.person_id = people.id AND roles.name = 'admin';
SELECT aspects.id, aspects.name, aspects.user_id, aspects.created_at, aspects.updated_at, aspects.order_id, aspects.post_default FROM aspects, roles, people WHERE roles.person_id = people.id AND roles.name = 'admin';
SELECT services.id, services.type, services.user_id, services.uid, services.access_token, services.access_secret, services.nickname, services.created_at, services.updated_at FROM services, roles, people WHERE roles.person_id = people.id AND roles.name = 'admin';
SELECT contacts.id, contacts.user_id, contacts.person_id, contacts.created_at, contacts.updated_at, contacts.sharing, contacts.receiving FROM contacts, roles, people WHERE roles.person_id = people.id AND roles.name = 'admin';
SELECT user_preferences.id, user_preferences.email_type, user_preferences.user_id, user_preferences.created_at, user_preferences.updated_at FROM user_preferences, roles, people WHERE roles.person_id = people.id AND roles.name = 'admin';
SELECT tag_followings.id, tag_followings.tag_id, tag_followings.user_id, tag_followings.created_at, tag_followings.updated_at FROM tag_followings, roles, people WHERE roles.person_id = people.id AND roles.name = 'admin';
SELECT reports.id, reports.item_id, reports.item_type, reports.reviewed, reports.text, reports.created_at, reports.updated_at, reports.user_id FROM reports, roles, people WHERE roles.person_id = people.id AND roles.name = 'admin';

-- visible_by_moderators
SELECT reports.id, reports.item_id, reports.item_type, reports.reviewed, reports.text, reports.created_at, reports.updated_at, reports.user_id, users.username FROM reports, users, roles, people WHERE reports.user_id = users.id AND roles.person_id = people.id AND roles.name = 'moderator';
SELECT posts.id, posts.author_id, posts."public", posts.guid, posts.type, posts.text, posts.created_at, posts.updated_at, posts.provider_display_name, posts.root_guid, posts.likes_count, posts.comments_count, posts.o_embed_cache_id, posts.reshares_count, posts.interacted_at, posts.tweet_id, posts.open_graph_cache_id, posts.tumblr_ids FROM reports, posts, roles, people WHERE reports.item_id = posts.id AND reports.item_type = 'Post' AND roles.person_id = people.id AND roles.name = 'moderator';
SELECT mentions.id, mentions.mentions_container_id, mentions.person_id, mentions.mentions_container_type FROM mentions, reports, posts, roles, people WHERE mentions.mentions_container_id = posts.id AND mentions.mentions_container_type = 'Post' AND reports.item_id = posts.id AND reports.item_type = 'Post' AND roles.person_id = people.id AND roles.name = 'moderator';
SELECT comments.id, comments.text, comments.commentable_id, comments.author_id, comments.guid, comments.created_at, comments.updated_at, comments.likes_count, comments.commentable_type FROM reports, comments, roles, people WHERE reports.item_id = comments.id AND reports.item_type = 'Comment' AND roles.person_id = people.id AND roles.name = 'moderator';
SELECT mentions.id, mentions.mentions_container_id, mentions.person_id, mentions.mentions_container_type FROM mentions, reports, comments, roles, people WHERE mentions.mentions_container_id = comments.id AND mentions.mentions_container_type = 'Comment' AND reports.item_id = comments.id AND reports.item_type = 'Comment' AND roles.person_id = people.id AND roles.name = 'moderator';
SELECT posts.id, posts.author_id, posts."public", posts.guid, posts.type, posts.text, posts.created_at, posts.updated_at, posts.provider_display_name, posts.root_guid, posts.likes_count, posts.comments_count, posts.o_embed_cache_id, posts.reshares_count, posts.interacted_at, posts.tweet_id, posts.open_graph_cache_id, posts.tumblr_ids FROM posts, reports, comments, roles, people WHERE comments.commentable_type = 'Post' AND comments.commentable_id = posts.id AND reports.item_id = comments.id AND reports.item_type = 'Comment' AND roles.person_id = people.id AND roles.name = 'moderator';
