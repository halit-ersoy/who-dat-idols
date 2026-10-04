package com.ses.whodatidols;

import com.ses.whodatidols.model.CastMemberDto;
import com.ses.whodatidols.model.Movie;
import com.ses.whodatidols.repository.ActorRepository;
import com.ses.whodatidols.repository.MovieRepository;
import com.ses.whodatidols.service.CastSyncService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class WhoDatIdolsApplicationTests {

    @Autowired
    private CastSyncService castSyncService;

    @Autowired
    private ActorRepository actorRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Test
    void contextLoads() {
    }

    @Test
    void testCastSyncAndPhotoRestoration() {
        List<Movie> movies = movieRepository.findAll();
        assertFalse(movies.isEmpty(), "Movies should not be empty");

        Movie testMovie = movies.stream()
                .filter(m -> m.getName() != null && !m.getName().trim().isEmpty())
                .findFirst()
                .orElse(null);
        assertNotNull(testMovie, "Should find at least one movie");

        List<CastMemberDto> cast = castSyncService.syncCastForMovie(testMovie, false, true);
        assertFalse(cast.isEmpty(), "Cast should not be empty for " + testMovie.getName());

        int photosFoundOnDisk = 0;
        for (CastMemberDto member : cast) {
            if (member.getActorId() != null && actorRepository.hasLocalPhoto(member.getActorId())) {
                photosFoundOnDisk++;
            }
        }
        assertTrue(photosFoundOnDisk > 0, "At least some actor photos should be saved on disk");
    }
}
