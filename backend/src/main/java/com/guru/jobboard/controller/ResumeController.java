package com.guru.jobboard.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.guru.jobboard.dto.PreferenceDto;
import com.guru.jobboard.dto.ResumeDto;
import com.guru.jobboard.dto.ResumeTextRequest;
import com.guru.jobboard.dto.SkillsRequest;
import com.guru.jobboard.security.AuthUser;
import com.guru.jobboard.service.ResumeService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/resume")
public class ResumeController {

    private final ResumeService resumeService;

    public ResumeController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    @GetMapping
    public ResumeDto get(@AuthenticationPrincipal AuthUser me) {
        return resumeService.get(me.id());
    }

    /** Upload a PDF, DOCX or TXT resume. */
    @PostMapping("/upload")
    public ResumeDto upload(@AuthenticationPrincipal AuthUser me, @RequestParam("file") MultipartFile file) {
        return resumeService.upload(me.id(), file);
    }

    /** Paste resume text. */
    @PutMapping("/text")
    public ResumeDto saveText(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody ResumeTextRequest request) {
        return resumeService.saveText(me.id(), request.text(), "Pasted text");
    }

    /** Edit the extracted skill list. */
    @PutMapping("/skills")
    public ResumeDto updateSkills(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody SkillsRequest request) {
        return resumeService.updateSkills(me.id(), request.skills());
    }

    /** Use the top resume skills as search keywords. */
    @PostMapping("/use-as-keywords")
    public PreferenceDto useAsKeywords(@AuthenticationPrincipal AuthUser me,
                                       @RequestParam(defaultValue = "6") int count) {
        return PreferenceDto.from(resumeService.useSkillsAsKeywords(me.id(), count));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthUser me) {
        resumeService.delete(me.id());
    }
}
