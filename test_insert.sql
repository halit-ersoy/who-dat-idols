DECLARE @cid UNIQUEIDENTIFIER = NEWID();
INSERT INTO VideoSource (ID, ContentId, SourceName, SourceUrl, SortOrder) 
VALUES (NEWID(), @cid, 'Vidmoly', '<iframe src="https://vkvideo.ru/video_ext.php?oid=-232761484&id=456240724&hash=6f082da1c7fe0467&hd=3" width="1280" height="720" allow="autoplay; encrypted-media; fullscreen; picture-in-picture; screen-wake-lock;" frameborder="0" allowfullscreen></iframe>', 0);
SELECT * FROM VideoSource WHERE SourceName = 'Vidmoly';
GO
