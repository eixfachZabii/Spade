//// src/main/java/com/pokerapp/api/controller/InvitationController.java
//package com.pokerapp.api.controller;
//
//import com.pokerapp.api.dto.request.InvitationRequestDto;
//import com.pokerapp.api.dto.response.InvitationDto;
//import com.pokerapp.domain.invitation.Invitation;
//import com.pokerapp.domain.user.User;
//import com.pokerapp.service.InvitationService;
//import com.pokerapp.service.UserService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import jakarta.validation.Valid;
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/invitations")
//public class InvitationController {
//
//    @Autowired
//    private InvitationService invitationService;
//
//    @Autowired
//    private UserService userService;
//
//    @PostMapping
//    public ResponseEntity<InvitationDto> sendInvitation(@Valid @RequestBody InvitationRequestDto requestDto) {
//        User currentUser = userService.getCurrentUser();
//        Invitation invitation = invitationService.createInvitation(requestDto, currentUser);
//        return ResponseEntity.ok(convertToDto(invitation));
//    }
//
//    @GetMapping("/received")
//    public ResponseEntity<List<InvitationDto>> getReceivedInvitations() {
//        User currentUser = userService.getCurrentUser();
//        return ResponseEntity.ok(invitationService.getPendingInvitationsForUser(currentUser));
//    }
//
//    @GetMapping("/sent")
//    public ResponseEntity<List<InvitationDto>> getSentInvitations() {
//        User currentUser = userService.getCurrentUser();
//        return ResponseEntity.ok(invitationService.getSentInvitationsForUser(currentUser));
//    }
//
//    @PostMapping("/{id}/accept")
//    public ResponseEntity<InvitationDto> acceptInvitation(@PathVariable Long id) {
//        User currentUser = userService.getCurrentUser();
//        return ResponseEntity.ok(invitationService.acceptInvitation(id, currentUser));
//    }
//
//    @PostMapping("/{id}/decline")
//    public ResponseEntity<InvitationDto> declineInvitation(@PathVariable Long id) {
//        User currentUser = userService.getCurrentUser();
//        return ResponseEntity.ok(invitationService.declineInvitation(id, currentUser));
//    }
//
//    private InvitationDto convertToDto(Invitation invitation) {
//        InvitationDto dto = new InvitationDto();
//        dto.setId(invitation.getId());
//        dto.setSenderId(invitation.getSender().getId());
//        dto.setSenderName(invitation.getSender().getUsername());
//        dto.setRecipientId(invitation.getRecipient().getId());
//        dto.setRecipientName(invitation.getRecipient().getUsername());
//        dto.setTableId(invitation.getTableId());
//        dto.setStatus(invitation.getStatus().toString());
//        dto.setMessage(invitation.getMessage());
//        dto.setCreatedAt(invitation.getCreatedAt().toString());
//        dto.setExpiresAt(invitation.getExpiresAt().toString());
//        return dto;
//    }
//}
