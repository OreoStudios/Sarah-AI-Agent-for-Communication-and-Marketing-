package com.oreostudios.sarah.prospecting.internal.infrastructure.web;
import com.oreostudios.sarah.prospecting.internal.application.AutoProspectingService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController
@RequestMapping("/api/v1/prospects/acquisition")
@PreAuthorize("hasRole('OWNER')")
public class AutoProspectingController {
    private final AutoProspectingService service;
    public AutoProspectingController(AutoProspectingService service){this.service=service;}
    public record Request(String query,String postalCode,String activityPrefix){}
    @PostMapping public AutoProspectingService.Result discover(@RequestBody Request request,Authentication auth){
        return service.run(request.query(),request.postalCode(),request.activityPrefix(),auth.getName());
    }
    @GetMapping public List<Map<String,Object>> history(){return service.history();}
}
