package com.example.LiveHost.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "Qcard")
public class Qcard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "qcard_id")
    private Long qcardId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "broadcast_id", nullable = false)
    private Broadcast broadcast;

    @Column(name = "qcard_question", length = 50, nullable = false)
    private String qcardQuestion;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}