package br.com.CadernoDeReceitas.repository;

import br.com.CadernoDeReceitas.model.ReceitaCulinaria;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.UnaryOperator;

@Repository
public final class InMemoryReceitaCulinariaRepository implements ReceitaCulinariaRepository {
    private final ConcurrentHashMap<Long, ReceitaCulinaria> receitas = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    @Override
    public ReceitaCulinaria create(String nome, float duracaoDoPreparoMinutos, ArrayList<String> ingredientes, String tutorialPreparo) {
        long id = sequence.incrementAndGet();
        var receita = new ReceitaCulinaria(id, nome, duracaoDoPreparoMinutos, ingredientes, tutorialPreparo);
        receitas.put(id, receita);
        return receita;
    }

    @Override
    public List<ReceitaCulinaria> findAll() {
        return receitas.values().stream()
                .sorted(Comparator.comparingLong(ReceitaCulinaria::id))
                .toList();
    }

    @Override
    public Optional<ReceitaCulinaria> findById(long id) {
        return Optional.ofNullable(receitas.get(id));
    }

    @Override
    public Optional<ReceitaCulinaria> update(long id, String nome, float duracaoDoPreparoMinutos, ArrayList<String> ingredientes, String tutorialPreparo) {
        return Optional.ofNullable(receitas.computeIfPresent(
                id,
                (key, old) -> new ReceitaCulinaria(id, nome, duracaoDoPreparoMinutos, ingredientes, tutorialPreparo)
        ));
    }

    @Override
    public Optional<ReceitaCulinaria> patch(long id, UnaryOperator<ReceitaCulinaria> update) {
        return Optional.ofNullable(receitas.computeIfPresent(id, (key, old) -> update.apply(old)));
    }

    @Override
    public boolean deleteById(long id) {
        return receitas.remove(id) != null;
    }
}
