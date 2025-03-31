package com.ses.whodatidols.controller;

import com.ses.whodatidols.model.SoapOpera;
import com.ses.whodatidols.repository.SoapOperaRepository;
import com.ses.whodatidols.viewmodel.VideoViewModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/soapoperas")
public class SoapOperaController {
    private final SoapOperaRepository soapOperaRepository;

    public SoapOperaController(SoapOperaRepository soapOperaRepository) {
        this.soapOperaRepository = soapOperaRepository;
    }

    @GetMapping("/recent")
    public ResponseEntity<List<VideoViewModel>> getRecentSoapOperas(
            @RequestParam(value = "day", defaultValue = "20") int day) {
        List<SoapOpera> recentSoapOperas = soapOperaRepository.findRecentSoapOperas(day);

        List<VideoViewModel> viewModels = recentSoapOperas.stream()
                .map(soapOpera -> {
                    VideoViewModel vm = new VideoViewModel();
                    vm.setId(soapOpera.getId().toString());
                    vm.setTitle(soapOpera.getName());
                    vm.setInfo(soapOpera.getYear() + " • " + soapOpera.getCategory() + " • " + soapOpera.getTime() + " dk");

                    String thumbnailUrl = "/api/images/soap-opera?id=" + soapOpera.getId();
                    vm.setThumbnailUrl(thumbnailUrl);

                    vm.setVideoUrl("/watch?id=" + soapOpera.getId());

                    return vm;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(viewModels);
    }
}